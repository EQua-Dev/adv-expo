package tech.sourceid.sid_address_verification.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import androidx.annotation.RequiresApi
import tech.sourceid.sid_address_verification.R
import tech.sourceid.sid_address_verification.data.requests.AddGeoTagRequest
import tech.sourceid.sid_address_verification.domain.ApiHelper
import java.time.Instant
import androidx.core.content.edit
import tech.sourceid.sid_address_verification.domain.cacheGeoTag
import tech.sourceid.sid_address_verification.domain.clearCachedGeoTags
import tech.sourceid.sid_address_verification.domain.getCachedGeoTags
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenManager
import tech.sourceid.sid_address_verification.utils.HelpMe.getParsedAddressFromCoordinates
import tech.sourceid.sid_address_verification.utils.HelpMe.isInternetAvailable
import java.time.format.DateTimeFormatter


class LocationForegroundService : Service() {

    private val apiHelper = ApiHelper(RetrofitBuilder.apiService)
    private val tokenManager = TokenManager(this)


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var job: Job? = null
    val client = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val apiKey = intent?.getStringExtra("apiKey") ?: ""
        val customerID = intent?.getStringExtra("customer") ?: ""
        val token = intent?.getStringExtra("token") ?: ""
        val refreshToken = intent?.getStringExtra("refreshToken") ?: ""

        val prefs = this.getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("apiKey", apiKey).putString("token", token)
                .putString("refreshToken", refreshToken)
                .putString("customerID", customerID)
        }

        tokenManager.saveAccessToken(token)
        tokenManager.saveRefreshToken(refreshToken)
        tokenManager.saveApiKey(apiKey)
        tokenManager.saveCustomerID(customerID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasForegroundServicePermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasForegroundServicePermission) {
                Log.e(
                    "LocationService",
                    "Missing FOREGROUND_SERVICE_LOCATION permission on Android 14+"
                )
                stopSelf()
                return START_NOT_STICKY
            }
        }


        startForegroundWithNotification()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        job = CoroutineScope(Dispatchers.IO).launch {
            val cachedApiKey = tokenManager.getApiKey() ?: apiKey
            val cachedAccessToken = tokenManager.getAccessToken() ?: token
            val cachedCustomerID = tokenManager.getCustomerID() ?: customerID
            try {
                // 1. Fetch customer address history
                val historyResponse =
                    apiHelper.fetchCustomerHistory(cachedApiKey, cachedAccessToken)
                if (!historyResponse.isSuccessful || historyResponse.body() == null) {
                    Log.e("LocationService", "Failed to fetch customer location history")
                    stopSelf()
                    return@launch
                }

                val pendingData = historyResponse.body()!!
                    .data
                    .firstOrNull { it.verificationStatus == "pending" }

                if (pendingData == null) {
                    Log.e("LocationService", "No pending address verification found.")
                    stopSelf()
                    return@launch
                }

                // 2. Determine the start timestamp
                val mostRecentLocationTimestamp: Long? = pendingData.metadata.locations
                    .maxByOrNull { location ->
                        Instant.parse(location.timestamp).toEpochMilli()
                    }
                    ?.let { location ->
                        Instant.parse(location.timestamp).toEpochMilli()
                    }

                val currentTime = mostRecentLocationTimestamp ?: System.currentTimeMillis()

                Log.d("LocationService", "Using start time: $currentTime")

                // 3. Fetch organisation config
                val configResponse = apiHelper.fetchOrganisationConfig(cachedApiKey)
                if (!configResponse.isSuccessful || configResponse.body() == null) {
                    Log.e("LocationService", "Failed to fetch org config")
                    stopSelf()
                    return@launch
                }

                val config = configResponse.body()!!.data
                val pollingIntervalHours = config.geotaggingPollingInterval // e.g. 2.5
//                val pollingIntervalHours = config.geotaggingPollingInterval / 285 // e.g. 2 minutes
                val sessionTimeoutDays = config.geotaggingSessionTimeout.toDouble() // e.g. 1.0

                val intervalMs = (pollingIntervalHours * 60 * 60 * 1000).toLong()
                val durationMs = (sessionTimeoutDays * 24 * 60 * 60 * 1000).toLong()

                val captureTimestamps = mutableListOf<Long>()
                var timestamp = currentTime
                val endTime = currentTime + durationMs

                while (timestamp <= endTime) {
                    captureTimestamps.add(timestamp)
                    timestamp += intervalMs
                }

                Log.d("LocationService", "Generated ${captureTimestamps.size} timestamps")
                captureTimestamps.forEach {
                    Log.d("LocationService", "Generated $it")
                }

                // Now loop through captureTimestamps and perform geotagging...
                for (ts in captureTimestamps) {

                    Log.d("LocationService", "Timestamp ${captureTimestamps.indexOf(ts)}: $ts")

//                    if (waitTime > 0) delay(waitTime)
                    val waitTime = ts - System.currentTimeMillis()
                    val roundedWaitTime =
                        ((waitTime + 59_999) / 60_000) * 60_000 // Round up to nearest minute

                    Log.d("LocationService", "wait time: $waitTime")
                    Log.d("LocationService", "rounded wait time: $roundedWaitTime")

                    if (roundedWaitTime > 0) {
                        delay(roundedWaitTime)
                        Log.d("LocationService", "Finished delay for $roundedWaitTime ms")

                    }

                    if (ActivityCompat.checkSelfPermission(
                            this@LocationForegroundService,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val location = fusedLocationClient.lastLocation.await()
                        Log.d("LocationService", "Location result: $location")

                        if (location == null) {
                            Log.w("LocationService", "Location is null, skipping this timestamp")
                        } else {
                            val parsedAddress = getParsedAddressFromCoordinates(
                                this@LocationForegroundService,
                                location.latitude,
                                location.longitude
                            )

                            Log.d("LocationService", "Parsed address: $parsedAddress")

                            val address = listOfNotNull(
                                parsedAddress.addressLineOne,
                                parsedAddress.addressLineTwo,
                                parsedAddress.city,
                                parsedAddress.region,
                                parsedAddress.country,
                                parsedAddress.zipCode
                            ).joinToString(" ").ifBlank { "Unknown Location" }

                            val deviceTimestamp =
                                DateTimeFormatter.ISO_INSTANT.format(Instant.now())

                            val geoTag = AddGeoTagRequest(
                                address = address,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                deviceTimestamp = deviceTimestamp
                            )

                            if (isInternetAvailable(this@LocationForegroundService)) {
                                // 1. Try sending cached ones first
                                val cachedGeoTags = getCachedGeoTags(this@LocationForegroundService)
                                var allSent = true
                                for (cached in cachedGeoTags) {
                                    try {
                                        val response = apiHelper.addGeoTag(apiKey, token, cached)
                                        if (!response.isSuccessful) {
                                            allSent = false
                                            break
                                        }
                                    } catch (e: Exception) {
                                        allSent = false
                                        break
                                    }
                                }

                                // If all cached tags sent successfully, clear cache
                                if (allSent) clearCachedGeoTags(this@LocationForegroundService)

                                // 2. Now send the current geoTag
                                try {
                                    val response =
                                        apiHelper.addGeoTag(cachedApiKey, cachedAccessToken, geoTag)

                                    if (response.isSuccessful) {
                                        Log.d("LocationService", "Location sent successfully")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@LocationForegroundService,
                                                "Location sent: ${location.latitude}, ${location.longitude}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Log.e(
                                            "LocationService",
                                            "Failed to send location: ${response.code()} - ${
                                                response.errorBody()?.string()
                                            }"
                                        )
                                        cacheGeoTag(this@LocationForegroundService, geoTag)

                                    }

                                } catch (e: Exception) {
                                    Log.e(
                                        "LocationService",
                                        "Error while sending location to server",
                                        e
                                    )
                                }
                            } else {

                                // Offline, cache the geotag
                                Log.w("LocationService", "Offline, caching location")
                                cacheGeoTag(this@LocationForegroundService, geoTag)
                            }

                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("LocationService", "Unhandled exception", e)

            }

            stopSelf() // stop service after duration
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        // Setup your persistent notification here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "location_channel"
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Verifying address")
                .setContentText("Sending location updates for verification...")
                .setSmallIcon(R.drawable.ic_location)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
