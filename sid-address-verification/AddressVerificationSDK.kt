package tech.sourceid.addressverification


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import tech.sourceid.addressverification.data.AddressVerificationConfig
import tech.sourceid.addressverification.services.startLocationTracking

@Composable
fun AddressVerificationSDK(
    config: AddressVerificationConfig,
    onSubmit: (String, Double, Double) -> Unit,
    onLocationPost: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }

    var query by remember { mutableStateOf(config.initialAddressText ?: "") }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }

    var selectedAddress by remember { mutableStateOf<Pair<String, LatLng>?>(null) }

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

    Column(Modifier.padding(16.dp)) {
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
                            }
                    }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            selectedAddress?.let { (address, latLng) ->
                onSubmit(address, latLng.latitude, latLng.longitude)

                if (config.verifyLocation) {
                    startLocationTracking(
                        context = context,
                        interval = config.locationFetchIntervalHours,
                        duration = config.locationFetchDurationDays,
                        onLocationPost = onLocationPost,
                        customerID = config.customerID,
                        apiKey = config.apiKey,
                        token = config.token
                    )
                }
            }
        }) {
            Text("Submit Address")
        }
    }
}