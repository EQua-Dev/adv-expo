package tech.sourceid.sid_address_verification.services

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import tech.sourceid.sid_address_verification.AddressVerificationInternal
import tech.sourceid.sid_address_verification.ui.LocationPickerScreen

class LocationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocationPickerScreen(
                onConfirm = { latLng, address ->
                    /*   val result = Intent().apply {
                           putExtra("latitude", latLng.latitude)
                           putExtra("longitude", latLng.longitude)
                           putExtra("address", address)
                       }
                       setResult(RESULT_OK, result)*/

                    AddressVerificationInternal.sendPickedLocation(
                        latLng.latitude,
                        latLng.longitude,
                        address
                    )
                    finish()
                },
                onClose = { finish() }
            )
        }
    }
}
