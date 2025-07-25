package tech.sourceid.sid_address_verification

import android.content.Context

object AppContextHolder {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext // ✅ Avoid memory leak
    }

    fun getContext(): Context = appContext
}
