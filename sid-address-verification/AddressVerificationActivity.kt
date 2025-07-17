package tech.sourceid.addressverification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme


import com.google.android.gms.maps.model.LatLng
import tech.sourceid.addressverification.data.AddressVerificationConfig


class AddressVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = intent.getParcelableExtra<AddressVerificationConfig>("SDK_CONFIG")
            ?: return // Exit early if null

        setContent {
            MaterialTheme {
                AddressVerificationSDK(
                    config = config,
                    onSubmit = { address, lat, lng ->

                        println("Selected address: $address ($lat, $lng)")
                        // You can finish the activity here or send result back
                        // finish()
                    },
                    onLocationPost = { lat, lng ->
                        println("Posting user location to API: $lat, $lng")
                        // You could do background work or use ViewModel
                    }
                )
            }
        }
    }
}
