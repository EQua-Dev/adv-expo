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
import tech.sourceid.sid_address_verification.AppContextHolder
import tech.sourceid.sid_address_verification.data.TokenBundle
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.data.responses.CustomerAddressHistoryData
import tech.sourceid.sid_address_verification.data.responses.CustomerAddressHistoryResponse
import tech.sourceid.sid_address_verification.data.responses.GetOrganisationConfigResponse
import tech.sourceid.sid_address_verification.data.responses.OrganisationConfigData
import tech.sourceid.sid_address_verification.domain.cacheGeoTag
import tech.sourceid.sid_address_verification.domain.clearCachedGeoTags
import tech.sourceid.sid_address_verification.domain.getCachedGeoTags
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenManager
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenRefreshService
import tech.sourceid.sid_address_verification.utils.HelpMe.getParsedAddressFromCoordinates
import tech.sourceid.sid_address_verification.utils.HelpMe.isInternetAvailable
import java.time.format.DateTimeFormatter


class LocationForegroundService : Service() {

    private lateinit var apiHelper: ApiHelper

    //    private lateinit var tokenManager: TokenManager
    private val tokenManager by lazy { TokenManager(this) }


    private val tokenRefreshService by lazy {
        TokenRefreshService(apiHelper, tokenManager)
    }


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var job: Job? = null
    val client = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null


    /*  @RequiresApi(Build.VERSION_CODES.O)
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
  */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppContextHolder.init(this)

        val apiKey = intent?.getStringExtra("apiKey") ?: ""
        apiHelper = ApiHelper(RetrofitBuilder.create(apiKey))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        apiHelper = ApiHelper((RetrofitBuilder.apiService))
//        tokenManager = TokenManager(this)

        saveTokensAndPreferences(intent)

        if (!checkForegroundServicePermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()


        launchGeoTaggingJob()

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

    private fun saveTokensAndPreferences(intent: Intent?) {
        val apiKey = intent?.getStringExtra("apiKey") ?: ""
        val customerID = intent?.getStringExtra("customerID") ?: ""
        val verificationGroupID = intent?.getStringExtra("verificationGroupID") ?: ""
//        val token = intent?.getStringExtra("token") ?: ""
//        val refreshToken = intent?.getStringExtra("refreshToken") ?: ""

        Log.d("LocationService", "saveTokensAndPreferences apikey: $apiKey")
        Log.d("LocationService", "saveTokensAndPreferences customerID: $customerID")
        Log.d(
            "LocationService",
            "saveTokensAndPreferences verificationGroupID: $verificationGroupID"
        )
//        Log.d("LocationService", "saveTokensAndPreferences token: $token")
//        Log.d("LocationService", "saveTokensAndPreferences refreshToken: $refreshToken")

        // Check if tokenManager is initialized
//        if (!::tokenManager.isInitialized) {
//            Log.e("LocationService", "TokenManager not initialized!")
//            return
//        }

        val prefs = getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("apiKey", apiKey)
//            putString("token", token)
//            putString("refreshToken", refreshToken)
            putString("customerID", customerID)
            putString("verificationGroupID", verificationGroupID)
        }

        // Add try-catch and detailed logging for TokenManager calls
        try {
            Log.d("LocationService", "About to call tokenManager.saveAccessToken")
//            tokenManager.saveAccessToken(token)
            Log.d("LocationService", "Completed tokenManager.saveAccessToken")

            Log.d("LocationService", "About to call tokenManager.saveRefreshToken")
//            tokenManager.saveRefreshToken(refreshToken)
            Log.d("LocationService", "Completed tokenManager.saveRefreshToken")

            Log.d("LocationService", "About to call tokenManager.saveApiKey")
            tokenManager.saveApiKey(apiKey)
            Log.d("LocationService", "Completed tokenManager.saveApiKey")

            Log.d("LocationService", "About to call tokenManager.saveCustomerID")
            tokenManager.saveCustomerID(customerID)
            Log.d("LocationService", "Completed tokenManager.saveCustomerID")

            Log.d("LocationService", "About to call tokenManager.saveVerificationGroupID")
            tokenManager.saveVerificationGroupID(verificationGroupID)
            Log.d("LocationService", "Completed tokenManager.saveVerificationGroupID")

        } catch (e: Exception) {
            Log.e("LocationService", "Error calling TokenManager methods", e)
        }
    }

    private fun checkForegroundServicePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.e(
                    "LocationService",
                    "Missing FOREGROUND_SERVICE_LOCATION permission on Android 14+"
                )
            }

            return hasPermission
        }
        return true
    }

    private suspend fun refreshTokenIfNeeded(): String? {
        return tokenRefreshService.refreshTokenIfNeeded()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun launchGeoTaggingJob() {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = this@LocationForegroundService
                val tokens = getCachedOrIntentTokens()

                val pendingData = getPendingVerification(tokens) ?: run {
                    stopSelf()
                    return@launch
                }

                val currentTime = getStartTimestamp(pendingData)
                val config = getOrganisationConfig(tokens) ?: run {
                    stopSelf()
                    return@launch
                }

                val captureTimestamps = generateCaptureTimestamps(currentTime, config)
                Log.d("LocationService", "Generated ${captureTimestamps.size} timestamps:")

                captureTimestamps.forEachIndexed { index, ts ->
                    val readable = Instant.ofEpochMilli(ts)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()

                    Log.d("LocationService", "[$index] $readable  ($ts)")
                }


                for (ts in captureTimestamps) {
                    handleGeoTagTimestamp(ts, tokens, context)
                    Log.d("LocationService", "launchGeoTaggingJob: $ts")
                }

            } catch (e: Exception) {
                Log.e("LocationService", "Unhandled exception", e)
            }

            stopSelf()
        }
    }

    private suspend fun getOrganisationConfig(tokens: TokenBundle): OrganisationConfigData? {
        val configResponse = apiHelper.fetchOrganisationConfig(tokens.apiKey)
        if (!configResponse.isSuccessful || configResponse.body() == null) {
            Log.e("LocationService", "Failed to fetch org config")
            return null
        }

        return configResponse.body()!!.data
    }


    private fun getCachedOrIntentTokens(): TokenBundle {

        val geoPrefs = getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)

        val apiKey = tokenManager.getApiKey().takeIf { !it.isNullOrBlank() }
            ?: geoPrefs.getString("apiKey", "") ?: ""

        val accessToken = tokenManager.getAccessToken().takeIf { !it.isNullOrBlank() }
            ?: geoPrefs.getString("token", "") ?: ""

        val customerID = tokenManager.getCustomerID().takeIf { !it.isNullOrBlank() }
            ?: geoPrefs.getString("customerID", "") ?: ""

        val verificationGroupID = tokenManager.getVerificationGroupID().takeIf { !it.isNullOrBlank() }
            ?: geoPrefs.getString("verificationGroupID", "") ?: ""


        Log.d("LocationService", "saveTokensAndPreferences apikey2: $apiKey")
        Log.d("LocationService", "saveTokensAndPreferences customerID2: $customerID")
        Log.d("LocationService", "saveTokensAndPreferences token2: $accessToken")
        Log.d("LocationService", "saveTokensAndPreferences verificationGroupID2: $verificationGroupID")
//        Log.d("LocationService", "saveTokensAndPreferences refreshToken2: $refreshToken")

        return TokenBundle(apiKey, accessToken, customerID, verificationGroupID)
    }

    private suspend fun getPendingVerification(tokens: TokenBundle): CustomerAddressHistoryData? {
        Log.d("LocationService", "getPendingVerification tokens: $tokens")
        val response = apiHelper.fetchCustomerHistory(
            tokens.apiKey,
            tokens.customerID,
            verificationGroupId = tokens.verificationGroupID
        )
        if (!response.isSuccessful || response.body() == null) {
            val errorBody = response.errorBody()?.string()
            Log.e(
                "LocationService",
                "Failed to fetch customer location history. " +
                        "Code: ${response.code()}, Message: ${response.message()}, Error: $errorBody"
            )
            return null
        }

        return response.body()!!.data.firstOrNull {
            it.verificationStatus == "pending"
        }.also {
            if (it == null) {
                Log.e("LocationService", "No pending address verification found.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getStartTimestamp(pendingData: CustomerAddressHistoryData): Long {
        return pendingData.metadata.locations
            .maxByOrNull { Instant.parse(it.timestamp).toEpochMilli() }
            ?.let { Instant.parse(it.timestamp).toEpochMilli() }
            ?: System.currentTimeMillis()
    }

/*
    private fun generateCaptureTimestamps(
        startTimestamp: Long,
        config: OrganisationConfigData
    ): List<Long> {
        val intervalMs = (config.geotaggingPollingInterval * 60 * 60 * 1000).toLong()
        val durationMs = (config.geotaggingSessionTimeout * 24 * 60 * 60 * 1000).toLong()

        val timestamps = mutableListOf<Long>()
        var current = startTimestamp
        val endTime = startTimestamp + durationMs

        while (current <= endTime) {
            timestamps.add(current)
            current += intervalMs
        }

        return timestamps
    }
*/

    private fun generateCaptureTimestamps(startTimestamp: Long, config: OrganisationConfigData): List<Long> {

        val devMode = false

        val intervalMs = if (devMode) {
            1 * 60 * 1000L   // 1 minutes
        } else {
            (config.geotaggingPollingInterval * 60 * 60 * 1000).toLong()
        }

        val durationMs = if (devMode) {
            10 * 60 * 1000L   // generate ~5 timestamps for testing
        } else {
            (config.geotaggingSessionTimeout * 24 * 60 * 60 * 1000).toLong()
        }

        val timestamps = mutableListOf<Long>()
        var current = startTimestamp
        val endTime = startTimestamp + durationMs

        while (current <= endTime) {
            timestamps.add(current)
            current += intervalMs
        }

        return timestamps
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun handleGeoTagTimestamp(
        timestamp: Long,
        tokens: TokenBundle,
        context: Context
    ) {
  /*      val waitTime = timestamp - System.currentTimeMillis()
        val roundedWaitTime = ((waitTime + 59_999) / 60_000) * 60_000

        if (roundedWaitTime > 0) {
            Log.d("LocationService", "Delaying for $roundedWaitTime ms")
            delay(roundedWaitTime)
        }*/

        val waitTime = timestamp - System.currentTimeMillis()

        if (waitTime > 0) {
            Log.d("LocationService", "Delaying EXACT wait time: $waitTime ms")
            delay(waitTime)
        }

        Log.d("LocationService", "handleGeoTagTimestamp: Past wait code")


        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val location = fusedLocationClient.lastLocation.await()
        if (location == null) {
            Log.w("LocationService", "Location is null, skipping timestamp")
            return
        }

        val parsedAddress = getParsedAddressFromCoordinates(
            context,
            location.latitude,
            location.longitude
        )

        val address = listOfNotNull(
            parsedAddress.addressLineOne,
            parsedAddress.addressLineTwo,
            parsedAddress.city,
            parsedAddress.region,
            parsedAddress.country,
            parsedAddress.zipCode
        ).joinToString(" ").ifBlank { "Unknown Location" }


        val geoTag = AddGeoTagRequest(
            address = address,
            latitude = location.latitude,
            longitude = location.longitude,
            customer = tokens.customerID,
            deviceTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )

        Log.d("LocationService", "handleGeoTagTimestamp: geoTag is $geoTag")


        if (isInternetAvailable(context)) {

            val cachedGeoTags = getCachedGeoTags(context)
            Log.d("LocationService", "handleGeoTagTimestamp: $cachedGeoTags")
            var allSent = true
            for (cached in cachedGeoTags) {
                try {
                    val response =
                        apiHelper.addGeoTag(tokens.apiKey, /*tokens.accessToken,*/ cached)
                    if (!response.isSuccessful) {
                        allSent = false
                        break
                    }
                } catch (e: Exception) {
                    allSent = false
                    break
                }
            }

            if (allSent) clearCachedGeoTags(context)

            try {
                val response = apiHelper.addGeoTag(tokens.apiKey, /*tokens.accessToken,*/ geoTag)
                if (response.isSuccessful) {
                    Log.d("LocationService", "Location sent successfully")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Location sent: ${location.latitude}, ${location.longitude}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("LocationService", "Failed to send location: ${response.code()}")
                    cacheGeoTag(context, geoTag)
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Error sending location", e)
                cacheGeoTag(context, geoTag)
            }
        } else {
            Log.w("LocationService", "Offline, caching location")
            cacheGeoTag(context, geoTag)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
