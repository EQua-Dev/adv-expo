package tech.sourceid.sid_address_verification.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.sourceid.sid_address_verification.AppContextHolder
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenAuthenticator
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenInterceptor
import tech.sourceid.sid_address_verification.services.tokenmanager.TokenManager

object RetrofitBuilder {
    private const val BASE_URL = "https://api.rd.usesourceid.com/v1/api/"

    private val tokenManager = TokenManager(AppContextHolder.getContext())

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(TokenInterceptor(tokenManager))
        .authenticator(TokenAuthenticator(tokenManager))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

}