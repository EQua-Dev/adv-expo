package tech.sourceid.sid_address_verification.services

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.sourceid.sid_address_verification.AppContextHolder
import tech.sourceid.sid_address_verification.SidEnvironment
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenAuthenticator
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenInterceptor
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenManager


object RetrofitBuilder {

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor { message -> Log.d("RetrofitLog", message) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun create(apiKey: String): ApiService {
        val baseUrl = SidEnvironment.resolveBaseUrl(apiKey)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
