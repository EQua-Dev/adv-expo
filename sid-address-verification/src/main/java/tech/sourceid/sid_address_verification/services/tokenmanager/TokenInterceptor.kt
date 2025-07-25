package tech.sourceid.sid_address_verification.services.tokenmanager

import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val newRequest = request.newBuilder()
            .addHeader("x-auth-token", accessToken!!)
            .build()

        return chain.proceed(newRequest)
    }
}
