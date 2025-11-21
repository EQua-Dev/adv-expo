package tech.sourceid.sid_address_verification.utils

import android.Manifest
import android.app.Service.CONNECTIVITY_SERVICE
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.RequiresPermission
import tech.sourceid.sid_address_verification.data.ParsedAddress
import java.util.Locale

object HelpMe {
    fun getParsedAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): ParsedAddress {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            Log.d("LocationService", "getParsedAddressFromCoordinates: $geocoder")
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                ParsedAddress(
                    country = address.countryName,
                    addressLineOne = address.subThoroughfare ?: address.featureName,
                    addressLineTwo = address.thoroughfare ?: address.getAddressLine(0),
                    city = address.locality ?: address.subAdminArea,
                    region = address.adminArea,
                    countryCode = address.countryCode,
                    postalCode = address.postalCode,
                    zipCode = address.postalCode // same as postalCode, used for clarity
                )
            } else {
                ParsedAddress()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedAddress()
        }
    }


    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

}