package tech.sourceid.addressverification

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tech.sourceid.addressverification.data.AddressVerificationConfig
import tech.sourceid.addressverification.services.LocationTracking
import tech.sourceid.addressverification.services.startLocationTracking
import java.net.HttpURLConnection
import java.net.URL

class AddressVerification(private val context: Context) {
    private val locationTracking by lazy { LocationTracking(context) }

    // Add this configuration data class
    data class Config(
        val geotaggingPollingInterval: Long,
        val geotaggingSessionTimeout: Long
    )

    fun startLocationTracking(
        apiKey: String,
        token: String,
        customerID: String,
        onLocationPost: (Double, Double) -> Unit
    ) {
        Log.d("AddressVerification", "startLocationTracking: internal first start tracking")
        locationTracking.startTracking(apiKey, token, customerID, onLocationPost)
    }

    fun stopLocationTracking() {
        locationTracking.stopTracking()
    }

    // Add this method to resolve the fetchConfig error
    fun fetchConfig(): Config {
        return Config(
            geotaggingPollingInterval = 5000L, // 5 seconds
            geotaggingSessionTimeout = 30000L // 30 seconds
        )
    }
}

@Composable
fun AddressVerificationField(
    modifier: Modifier = Modifier,
    apiKey: String,
    token: String = "",
    showButton: Boolean,
    initialText: String = "",
    verifyLocation: Boolean = false,
    customerID: String,
    onAddressSelected: (String, Double, Double) -> Unit,
    onLocationPost: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }

    var query by remember { mutableStateOf(initialText) }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var selectedAddress by remember { mutableStateOf<Pair<String, LatLng>?>(null) }

    var pollingInterval by remember { mutableStateOf<Double?>(null) }
    var sessionTimeout by remember { mutableStateOf<Double?>(null) }

    // NEW: loading flag
    var loading by remember { mutableStateOf(true) }

    // Fetch config when the composable starts
    LaunchedEffect(apiKey) {
        try {
            withContext(Dispatchers.IO) {
                val url =
                    URL("https://api.rd.usesourceid.com/v1/api/organization/address-verification-config")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("accept", "*/*")
                    setRequestProperty("x-api-key", apiKey)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val data = json.getJSONObject("data")

                    Log.d("TAG", "AddressVerificationField: $json")

                    withContext(Dispatchers.Main) {
                        pollingInterval = data.getDouble("geotaggingPollingInterval")
                        sessionTimeout = data.getInt("geotaggingSessionTimeout").toDouble()
                        loading = false // ✅ Done loading
                    }
                } else {
                    Log.e("AddressVerificationField", "Error: HTTP $responseCode")
                    withContext(Dispatchers.Main) {
                        loading = false // mark done even on error
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loading = false // mark done even on exception
        }
    }

    // ✅ SHOW LOADING OR MAIN UI
    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {

        LaunchedEffect(query) {
            if (query.isNotBlank()) {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setSessionToken(AutocompleteSessionToken.newInstance())
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        suggestions = response.autocompletePredictions
                    }
            }
        }
        // --- Your existing UI ---
        Column(
            modifier
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Enter your address") },
                modifier = Modifier.fillMaxWidth()
            )

            suggestions.forEach { prediction ->
                Text(
                    text = prediction.getFullText(null).toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val placeId = prediction.placeId
                            val placeFields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG)

                            val request = FetchPlaceRequest
                                .builder(placeId, placeFields)
                                .build()

                            placesClient
                                .fetchPlace(request)
                                .addOnSuccessListener { response ->
                                    val place = response.place
                                    selectedAddress = Pair(place.address!!, place.latLng!!)
                                    query = place.address!!
                                    suggestions = emptyList()
                                    onAddressSelected(
                                        selectedAddress!!.first,
                                        selectedAddress!!.second.latitude,
                                        selectedAddress!!.second.longitude
                                    )
                                }
                        }
                        .padding(8.dp)
                )
            }

            if (showButton){
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        selectedAddress?.let { (address, latLng) ->
                            onAddressSelected(address, latLng.latitude, latLng.longitude)

                            if (verifyLocation) {
                                val interval = pollingInterval ?: 0.5
                                val timeout = sessionTimeout ?: 1.0
                                startLocationTracking(
                                    context = context,
                                    interval = interval,
                                    duration = timeout,
                                    customerID = customerID,
                                    apiKey = apiKey,
                                    token = token,
                                    onLocationPost = onLocationPost
                                )
                            }
                        }
                    }) {
                        Text("Submit Address")
                    }
                }
            }

        }
    }
}
