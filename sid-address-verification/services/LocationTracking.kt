package tech.sourceid.addressverification.services


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import tech.sourceid.addressverification.data.AddressVerificationConfig
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("MissingPermission")
fun startLocationTracking(
    context: Context,
    interval: Double,
    duration: Double,
    customerID: String,
    apiKey: String,
    token: String,
    onLocationPost: (Double, Double) -> Unit
) {
/*
    val intent = Intent(context, LocationForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    CoroutineScope(Dispatchers.IO).launch {
        val repeatCount = duration / interval
        repeat(repeatCount.toInt()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        onLocationPost(it.latitude, it.longitude)
                    }
                }
            delay(interval * 60 * 1000) // convert minutes to milliseconds
        }
    }*/
    val intent = Intent(context, LocationForegroundService::class.java).apply {
        putExtra("interval", interval)
        putExtra("duration", duration)
        putExtra("customer", customerID)
        putExtra("apiKey", apiKey)
        putExtra("token", token)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}


class LocationTracking(private val context: Context) {

    private var trackingJob: Job? = null

    suspend fun fetchConfig(apiKey: String): AddressVerificationConfig {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.rd.usesourceid.com/v1/api/organization/address-verification-config")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("accept", "*/*")
                setRequestProperty("x-api-key", apiKey)
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val data = json.getJSONObject("data")

            Log.d("LocationTracking", "fetchConfig: ${data}")

            val interval = data.optDouble("geotaggingPollingInterval")
                .takeIf { !it.isNaN() && it != 0.0 } ?: 0.5

            val duration = data.optInt("geotaggingSessionTimeout").takeIf { it != 0 } ?: 1

            AddressVerificationConfig(
                locationFetchIntervalHours = interval,
                locationFetchDurationDays = duration.toDouble()
            )
        }
    }

    fun startTracking(
        apiKey: String,
        token: String,
        customerID: String,
        onLocationPost: (Double, Double) -> Unit
    ) {
        Log.d("AddressVerification", "startLocationTracking: internal second start tracking")
        trackingJob?.cancel()

        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = fetchConfig(apiKey)
                Log.d("AddressVerification", "startTracking: $config")
                startLocationTrackingInternal(
                    apiKey = apiKey,
                    token = token,
                    interval = config.locationFetchIntervalHours ?: 0.5,
                    duration = config.locationFetchDurationDays ?: 1.0,
                    customerID = customerID,
                    onLocationPost = onLocationPost
                )

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        val intent = Intent(context, LocationForegroundService::class.java)
        context.stopService(intent)
    }

    private fun startLocationTrackingInternal(
        apiKey: String,
        token: String,
        interval: Double,
        duration: Double,
        customerID: String,
        onLocationPost: (Double, Double) -> Unit
    ) {

        Log.d("LocationTracking", "startLocationTrackingInternal: $customerID")
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            putExtra("interval", interval)
            putExtra("duration", duration)
            putExtra("customer", customerID)
            putExtra("apiKey", apiKey)
            putExtra("token", token)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
