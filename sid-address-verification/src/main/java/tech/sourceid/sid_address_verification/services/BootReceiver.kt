package tech.sourceid.sid_address_verification.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession.Token
import android.os.Build
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val tokenManager = TokenManager(context)
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // You can optionally check for user preferences or app logic
            val prefs = context.getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)
            val apiKey = tokenManager.getApiKey()// prefs.getString("apiKey", "") ?: ""
            val token = tokenManager.getAccessToken()//prefs.getString("token", "") ?: ""
            val refreshToken = tokenManager.getRefreshToken()//prefs.getString("token", "") ?: ""
            val customerID = tokenManager.getCustomerID()//prefs.getString("customerID", "") ?: ""

            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                putExtra("apiKey", apiKey)        // these values can be stored in sharedPrefs
                putExtra("token", token)
                putExtra("customer", customerID)
                putExtra("refreshToken", refreshToken)
            }

            // Start foreground service after boot
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
