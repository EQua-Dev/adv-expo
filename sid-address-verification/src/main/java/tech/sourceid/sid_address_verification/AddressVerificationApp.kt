package tech.sourceid.sid_address_verification

import android.app.Application
import com.google.android.libraries.places.api.Places

class AddressVerificationApp: Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)

        Places.initialize(applicationContext, "AIzaSyD61uSS05BewaP-7NZ5LNDSnQ_D0yv-_Dk")
    }
}