package tech.sourceid.sid_address_verification.services


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import tech.sourceid.sid_address_verification.data.AddressVerificationConfig
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
    refreshToken: String,
    onLocationPost: (Double, Double) -> Unit
) {
    val intent = Intent(context, LocationForegroundService::class.java).apply {
        putExtra("interval", interval)
        putExtra("duration", duration)
        putExtra("customer", customerID)
        putExtra("apiKey", apiKey)
        putExtra("token", token)
        putExtra("refreshToken", refreshToken)
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
        refreshToken: String,
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
                    refreshToken = refreshToken,
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
        refreshToken: String,
        onLocationPost: (Double, Double) -> Unit
    ) {

        Log.d("LocationTracking", "startLocationTrackingInternal: $customerID")
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            putExtra("interval", interval)
            putExtra("duration", duration)
            putExtra("customer", customerID)
            putExtra("apiKey", apiKey)
            putExtra("token", token)
            putExtra("refreshToken", refreshToken)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
