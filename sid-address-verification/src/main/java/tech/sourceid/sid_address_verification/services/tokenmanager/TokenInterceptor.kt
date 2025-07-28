package tech.sourceid.sid_address_verification.services.tokenmanager

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {


    override fun intercept(chain: Interceptor.Chain): Response {


        val request = chain.request()
        val accessToken = tokenManager.getAccessToken()

        // Only add token if it exists
        val newRequest = if (accessToken != null) {
            request.newBuilder()
//                .addHeader("x-auth-token", accessToken)
                .build()
        } else {
            request // Proceed without token for public endpoints
        }

        return chain.proceed(newRequest)
    }
}
