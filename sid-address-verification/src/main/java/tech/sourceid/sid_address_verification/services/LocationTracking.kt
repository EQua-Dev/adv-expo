package tech.sourceid.sid_address_verification.services


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import tech.sourceid.sid_address_verification.data.AddressVerificationConfig
import tech.sourceid.sid_address_verification.domain.ApiHelper
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


class LocationTracking(
    private val context: Context
) {

    private var trackingJob: Job? = null
    private fun api(apiKey: String): ApiHelper {
        return ApiHelper(RetrofitBuilder.create(apiKey))
    }


    suspend fun fetchConfig(apiKey: String): AddressVerificationConfig {
        return withContext(Dispatchers.IO) {
            try {
                val api = api(apiKey)
                val response = api.fetchOrganisationConfig(apiKey)

                if (!response.isSuccessful) {
                    Log.e(
                        "LocationTracking",
                        "fetchConfig error: ${response.code()} ${response.message()}"
                    )
                    throw Exception("Failed to fetch config")
                }


                val body = response.body()
                val data = body?.data

                Log.d("LocationTracking", "Config response: $data")

                val interval = data?.geotaggingPollingInterval
                    ?.takeIf { it != 0.0 } ?: 0.5

                val duration = data?.geotaggingPollingInterval
                    ?.takeIf { it != 0.0 } ?: 1.0

//                AddressVerificationConfig(interval, duration)
                AddressVerificationConfig(locationFetchIntervalHours = interval, locationFetchDurationDays = duration.toDouble() )

            } catch (e: Exception) {
                Log.e("LocationTracking", "fetchConfig exception: ${e.localizedMessage}")

                // fallback values
                AddressVerificationConfig(
                    locationFetchIntervalHours = 0.5,
                    locationFetchDurationDays = 1.0
                )
            }
        }
    }

    fun startTracking(
        apiKey: String,
//        token: String,
        customerID: String,
        verificationGroupID: String? = null,
//        refreshToken: String,
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
//                    token = token,
                    customerID = customerID,
                    verificationGroupID = verificationGroupID,
                    interval = config.locationFetchIntervalHours ?: 0.5,
                    duration = config.locationFetchDurationDays ?: 1.0,
//                    refreshToken = refreshToken,
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
//        token: String,
        customerID: String,
        verificationGroupID: String?,
        interval: Double,
        duration: Double,
//        refreshToken: String,
        onLocationPost: (Double, Double) -> Unit
    ) {

        Log.d(
            "AddressVerification",
            "startLocationTrackingInternal: apiKey: $apiKey, customerID: $customerID, verificationGroupID: $verificationGroupID, interval: $interval, duration: $duration"
        )

        val intent = Intent(context, LocationForegroundService::class.java).apply {
            putExtra("interval", interval)
            putExtra("duration", duration)
            putExtra("apiKey", apiKey)
//            putExtra("token", token)
            putExtra("customerID", customerID)
            putExtra("verificationGroupID", verificationGroupID)
//            putExtra("refreshToken", refreshToken)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
