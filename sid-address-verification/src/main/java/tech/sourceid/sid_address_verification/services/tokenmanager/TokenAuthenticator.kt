package tech.sourceid.sid_address_verification.services.tokenmanager

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.services.ApiService


class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite loop
        if (responseCount(response) >= 2) {
            tokenManager.clearTokens() // Clear invalid tokens
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null
        val apiKey = tokenManager.getApiKey() ?: return null

        // Create a separate Retrofit instance without authenticator to avoid recursion
        val refreshRetrofit = Retrofit.Builder()
            .baseUrl("https://api.rd.usesourceid.com/v1/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val refreshApiService = refreshRetrofit.create(ApiService::class.java)

        return try {
            // Make synchronous call to refresh token
            val newTokenResponse = runBlocking {
                refreshApiService.refreshToken(
                    apiKey = apiKey,
                    postBody = RefreshTokenRequest(refreshToken)
                )
            }

            Log.d("Location", "refresh token api key: $apiKey")
            Log.d("Location", "refresh token: $newTokenResponse")

            if (newTokenResponse.isSuccessful) {
                val tokenData = newTokenResponse.body()?.data
                Log.d("Location", "refresh token: $tokenData")
                val newAccessToken = tokenData?.accessToken
                val newRefreshToken = tokenData?.refreshToken

                if (newAccessToken != null) {
                    // Save new tokens
                    tokenManager.saveAccessToken(newAccessToken)
                    if (newRefreshToken != null) {
                        tokenManager.saveRefreshToken(newRefreshToken)
                    }

                    // Retry the original request with new token
                    response.request.newBuilder()
                        .header("x-auth-token", newAccessToken)
                        .build()
                } else {
                    tokenManager.clearTokens()
                    null
                }
            } else {
                tokenManager.clearTokens()
                null
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var current = response.priorResponse
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }
}