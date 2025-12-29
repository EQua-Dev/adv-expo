package tech.sourceid.sid_address_verification

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


@Composable
fun AddressVerificationTest() {

    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(text = "AddressVerification Test")

            val apiKey = "sk_xxx_xx_xxxxxxxxxxxxxxxxx"
            val customerID = "xxxxxxxxxxxxxx"
            val verificationGroupID = "xxxxxxxxxxxxxxx"

            Button(onClick = {
                Log.d("TAG", "AddressVerificationTest: $apiKey")
                AddressVerification(context).startLocationTracking(
                    apiKey = apiKey,
                    customerID = customerID,
                    verificationGroupID = verificationGroupID
//                    token = token,
//                    refreshToken = refreshToken
                ) { lng, lat ->
                    Toast.makeText(context, "longitude: $lng, latitude: $lat", Toast.LENGTH_LONG)
                        .show()
                }
            }) {
                Text("Start Tracking")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                AddressVerification(context).pickLocation { address ->
                    Toast.makeText(
                        context,
                        "Picked: ${address.latitude}, ${address.longitude}\n${address.country}\n${address.state}\n${address.city}\n${address.street}\n${address.postalCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }) {
                Text("Pick Location")
            }

        }
    }
}
