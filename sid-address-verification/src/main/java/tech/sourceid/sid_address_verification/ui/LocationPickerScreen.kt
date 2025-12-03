package tech.sourceid.sid_address_verification.ui

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import java.util.Locale

@Composable
fun LocationPickerScreen(
    onConfirm: (LatLng, String) -> Unit,
    onClose: () -> Unit
) {
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {

        // Map UI
        GoogleMap(
            modifier = Modifier.weight(1f),
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
                    val address = reverseGeocode(context, latLng)
                    onConfirm(latLng, address)
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

