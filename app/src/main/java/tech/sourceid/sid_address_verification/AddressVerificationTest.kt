package tech.sourceid.sid_address_verification

import android.util.Log
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
            Text(text = "AddressVerification Test")

            val apiKey = "sk_sbx_v1_8YIM3v1WbgH4eVC2w7ZZRvpViK8YyE"
//            val apiKey = "sk_rd_v1_ChoPcUQjtI9pMTivjYJ9hKXop0WeXO"
            val token =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY4OTZlZjk3NDY0YWI2ODBiMjZiMzJmZCIsImVtYWlsIjoibHVvbXk1MisyQGdtYWlsLmNvbSIsInR5cGUiOiJDdXN0b21lciIsImlhdCI6MTc1NDcyMjI0NCwiZXhwIjoxNzU0ODA4NjQ0fQ.y4362If1lBDiwmKwU0or5p4ZshESFSJ8Rils5vsfhyE"
            val refreshToken =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY4OGZhNTcwMzU2ZDczNmRjOTcyY2RhNyIsIm9yZ2FuaXphdGlvbiI6IjY3Y2I2ODM5ODM2ZWYyNmNmZjVjZmVlYSIsImlhdCI6MTc1NTE4NDczMywiZXhwIjoxNzU3Nzc2NzMzfQ.VIfGSwcl6zzqKCvG0CZiqBBUyJQpZZoi-7R3uJpvyX4"

//            val customerID = "69202c22865447221c55ab7f"
            val customerID = "6924aa463c2063092b5012f6"
//            val verificationGroupID = "689209fc5679c2b5c30e19e4"
            val verificationGroupID = "68f0dc57f80227c0891a060e"

      /*      AddressVerification(context).startLocationTracking(
                apiKey = apiKey,
                customerID = customerID,
                verificationGroupID = verificationGroupID
//                token = token,
//                refreshToken = refreshToken
            ) { lng, lat ->

            }
*/
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
        }
    }
}
