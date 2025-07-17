package tech.sourceid.sid_address_verification.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // You can optionally check for user preferences or app logic
            val prefs = context.getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("apiKey", "") ?: ""
            val token = prefs.getString("token", "") ?: ""
            val customerID = prefs.getString("customerID", "") ?: ""

            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                putExtra("apiKey", apiKey)        // these values can be stored in sharedPrefs
                putExtra("token", token)
                putExtra("customer", customerID)
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
