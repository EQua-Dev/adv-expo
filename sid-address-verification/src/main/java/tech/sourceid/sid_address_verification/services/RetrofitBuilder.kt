package tech.sourceid.sid_address_verification.services

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.sourceid.sid_address_verification.AppContextHolder
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

    private fun resolveBaseUrl(apiKey: String): String {
        return when {
            apiKey.startsWith("sk_live_v1_") -> "https://api.sourceid.tech/v1/api/"
            apiKey.startsWith("sk_sbx_v1_")  -> "https://api.sbx.sourceid.tech/v1/api/"
            apiKey.startsWith("sk_uat_v1_")  -> "https://api.uat.usesourceid.com/v1/api/"
            apiKey.startsWith("sk_rd_v1_")   -> "https://api.rd.usesourceid.com/v1/api/"
            else -> throw IllegalArgumentException("Invalid API key: unknown environment")
        }
    }

    fun create(apiKey: String): ApiService {
        val baseUrl = resolveBaseUrl(apiKey)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

/*

object RetrofitBuilder {
    private const val BASE_URL = "https://api.rd.usesourceid.com/v1/api/"

    private val tokenManager by lazy { TokenManager(AppContextHolder.getContext()) }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor { message -> Log.d("RetrofitLog", message) }.apply {
            level = HttpLoggingInterceptor.Level.BODY // FULL logs
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // ✅ Log requests/responses
//            .addInterceptor(TokenInterceptor(tokenManager))
//            .authenticator(TokenAuthenticator(tokenManager))
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}

private fun resolveBaseUrl(apiKey: String): String {
    return when {
        apiKey.startsWith("sk_live_") -> "https://api.sourceid.tech/v1/api/"
        apiKey.startsWith("sk_sbx_")  -> "https://api.sbx.sourceid.tech/v1/api/"
        apiKey.startsWith("sk_uat_")  -> "https://api.uat.usesourceid.com/v1/api/"
        apiKey.startsWith("sk_rd_")   -> "https://api.rd.usesourceid.com/v1/api/"
        else -> throw IllegalArgumentException("Invalid API key: unknown environment")
    }
}
*/
