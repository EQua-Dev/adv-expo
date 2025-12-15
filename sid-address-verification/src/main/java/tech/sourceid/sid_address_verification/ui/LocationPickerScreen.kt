package tech.sourceid.sid_address_verification.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.sourceid.sid_address_verification.data.mappicker.ResolvedAddress
import java.util.Locale
import kotlin.coroutines.resume

@Composable
fun LocationPickerScreen(
    onConfirm: (ResolvedAddress) -> Unit,
    onClose: () -> Unit
) {
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    var hasSetInitial by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(selectedLatLng) {
        selectedLatLng?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 16f), // 16f = strong street-level zoom
                durationMs = 1200
            )
        }
    }

    // Get current location once
    LaunchedEffect(true) {
        val loc = getCurrentLocation(context)
        if (loc != null && !hasSetInitial) {
            selectedLatLng = LatLng(loc.latitude, loc.longitude)

            hasSetInitial = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map UI
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedLatLng = latLng
            }
        ) {
            selectedLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Selected location"
                )
            }
        }
        // Confirmation button
        Button(
            onClick = {
                selectedLatLng?.let { latLng ->
                    val address = reverseGeocodeFull(context, latLng)
                    onConfirm(address)
//                    val address = reverseGeocode(context, latLng)
//                    onConfirm(latLng, address)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Confirm Location")
        }
    }
}

fun reverseGeocode(context: Context, latLng: LatLng): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val result = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        result?.firstOrNull()?.getAddressLine(0) ?: ""
    } catch (e: Exception) {
        ""
    }
}

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): android.location.Location? {
    return suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        Log.d("LocationPicker", "getCurrentLocation: $client")
        client.lastLocation
            .addOnSuccessListener {
                Log.d("LocationPicker", "getCurrentLocation: $it")
                cont.resume(it) }
            .addOnFailureListener {
                Log.d("LocationPicker", "getCurrentLocation: $it")
                cont.resume(null) }
    }
}

fun reverseGeocodeFull(context: Context, latLng: LatLng): ResolvedAddress {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val result = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val addr = result?.firstOrNull()

        Log.d("LocationPicker", "reverseGeocodeFull: $result")
        
        ResolvedAddress(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            fullAddress = addr?.getAddressLine(0) ?: "",
            country = addr?.countryName,
            state = addr?.adminArea,
            city = addr?.locality ?: addr?.subAdminArea,
            postalCode = addr?.postalCode,
            street = addr?.thoroughfare
        )
    } catch (e: Exception) {
        ResolvedAddress(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            fullAddress = "",
            country = null, state = null, city = null,
            postalCode = null, street = null
        )
    }
}



