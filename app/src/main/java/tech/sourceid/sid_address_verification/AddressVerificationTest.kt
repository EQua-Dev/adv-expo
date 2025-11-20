package tech.sourceid.sid_address_verification

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext


@Composable
fun AddressVerificationTest() {

    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(text = "AddressVerificationTest")

            val apiKey = "sk_rd_v1_ChoPcUQjtI9pMTivjYJ9hKXop0WeXO"
            val token =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY4OTZlZjk3NDY0YWI2ODBiMjZiMzJmZCIsImVtYWlsIjoibHVvbXk1MisyQGdtYWlsLmNvbSIsInR5cGUiOiJDdXN0b21lciIsImlhdCI6MTc1NDcyMjI0NCwiZXhwIjoxNzU0ODA4NjQ0fQ.y4362If1lBDiwmKwU0or5p4ZshESFSJ8Rils5vsfhyE"
            val refreshToken =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY4OGZhNTcwMzU2ZDczNmRjOTcyY2RhNyIsIm9yZ2FuaXphdGlvbiI6IjY3Y2I2ODM5ODM2ZWYyNmNmZjVjZmVlYSIsImlhdCI6MTc1NTE4NDczMywiZXhwIjoxNzU3Nzc2NzMzfQ.VIfGSwcl6zzqKCvG0CZiqBBUyJQpZZoi-7R3uJpvyX4"

            val customerID = ""
            val verificationGroupID = ""

            AddressVerification(context).startLocationTracking(
                apiKey = apiKey,
                customerID = customerID,
                verificationGroupID = verificationGroupID
//                token = token,
//                refreshToken = refreshToken
            ) { lng, lat ->

            }

            Button(onClick = {
                AddressVerification(context).startLocationTracking(
                    apiKey = apiKey,
                    customerID = customerID,
//                    token = token,
//                    refreshToken = refreshToken
                ) { lng, lat ->
                    Toast.makeText(context, "longitude: $lng, latitude: $lat", Toast.LENGTH_LONG)
                        .show()
                }
            }) {
                Text("Start Tracking")
            }
        }
    }
}
