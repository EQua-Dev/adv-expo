package tech.sourceid.addressverification.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.sourceid.addressverification.R
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import kotlin.math.log
import android.location.Geocoder
import android.location.Address
import java.util.Locale


class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var job: Job? = null
    val client = OkHttpClient()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val interval = intent?.getLongExtra("interval", 15L) ?: 15L
//        val duration = intent?.getLongExtra("duration", 60L) ?: 60L
        val rawInterval = intent?.getDoubleExtra("interval", -1.0) ?: -1.0
        val intervalHours =
            if (rawInterval <= 0.0) 0.5 else rawInterval // fallback to 0.5 if zero or negative

        val rawDuration = intent?.getDoubleExtra("duration", -1.0) ?: -1.0
        val durationDays =
            if (rawDuration <= 0.0) 1.0 else rawDuration  // fallback to 1.0 if zero or negative

        val apiKey = intent?.getStringExtra("apiKey") ?: ""
        val customerID = intent?.getStringExtra("customer") ?: ""
        val token = intent?.getStringExtra("token") ?: ""

        val intervalMs = (intervalHours * 60 * 60 * 1000).toLong()
        val durationMs = (durationDays * 24 * 60 * 60 * 1000).toLong()

        val repeatCount = if (intervalMs > 0) (durationMs / intervalMs).toInt() else 1


        Log.d("LocationForegroundService", "onStartCommand interval: $intervalHours")
        Log.d("LocationForegroundService", "onStartCommand duration: $durationDays")
        Log.d("LocationForegroundService", "onStartCommand customerID: $customerID")

        startForegroundWithNotification()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        job = CoroutineScope(Dispatchers.IO).launch {
//            val repeatCount = duration / interval
//            val repeatCount = (duration * 1000) / interval
            repeat(repeatCount) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@LocationForegroundService,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val location = fusedLocationClient.lastLocation.await()
                        location?.let {
                            val parsedAddress = getParsedAddressFromCoordinates(
                                this@LocationForegroundService,
                                it.latitude,
                                it.longitude
                            )


                            // TODO: Replace with your location posting callback logic
                            // ✅ Send to server
                            val json = """
                        {
  "identity": "$customerID",
  "longitude": ${it.longitude},
  "latitude": ${it.latitude},
  "address": "${parsedAddress.addressLineOne} ${parsedAddress.addressLineTwo}",
}
                    """.trimIndent()

                            val requestBody =
                                json.toRequestBody("application/json".toMediaTypeOrNull())
                            val request = Request.Builder()
                                .url("https://api.rd.usesourceid.com/v1/api/customer/add-geotag")
                                .addHeader("x-api-key", apiKey)
                                .addHeader("x-auth-token", token)
                                .post(requestBody)
                                .build()

                            println("API Response: ${request}")
                            val response = OkHttpClient().newCall(request).execute()
                            val responseBody = response.body?.string()

                            println("API Response: ${responseBody}")
                            println("API Response: ${response.code}")

                            println("Location: ${it.latitude}, ${it.longitude}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@LocationForegroundService,
                                    "Sending Location from SourceID SDK: ${it.latitude}, ${it.longitude}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

//                delay(interval * 60 * 1000) // interval in minutes
                delay(intervalMs) // interval in minutes
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
                .setContentTitle("Tracking location")
                .setContentText("Sending location updates...")
                .setSmallIcon(R.drawable.ic_location)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    data class ParsedAddress(
        val country: String? = null,
        val addressLineOne: String? = null,
        val addressLineTwo: String? = null,
        val city: String? = null,
        val region: String? = null,
        val countryCode: String? = null,
        val postalCode: String? = null,
        val zipCode: String? = null
    )


    fun getParsedAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): ParsedAddress {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                ParsedAddress(
                    country = address.countryName,
                    addressLineOne = address.thoroughfare ?: address.getAddressLine(0),
                    addressLineTwo = address.subThoroughfare ?: address.featureName,
                    city = address.locality ?: address.subAdminArea,
                    region = address.adminArea,
                    countryCode = address.countryCode,
                    postalCode = address.postalCode,
                    zipCode = address.postalCode // same as postalCode, used for clarity
                )
            } else {
                ParsedAddress()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedAddress()
        }
    }

}
