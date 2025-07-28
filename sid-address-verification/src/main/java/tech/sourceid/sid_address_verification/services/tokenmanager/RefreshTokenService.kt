package tech.sourceid.sid_address_verification.services.tokenmanager

import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.domain.ApiHelper

class TokenRefreshService(
    private val apiHelper: ApiHelper,
    private val tokenManager: TokenManager
) {
    suspend fun refreshTokenIfNeeded(): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        val apiKey = tokenManager.getApiKey() ?: return null

        return try {
            val response = apiHelper.refreshToken(
                apiKey = apiKey,
                request = RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful) {
                val tokenData = response.body()?.data
                val newAccessToken = tokenData?.accessToken
                val newRefreshToken = tokenData?.refreshToken

                if (newAccessToken != null) {
                    tokenManager.saveAccessToken(newAccessToken)
                    if (newRefreshToken != null) {
                        tokenManager.saveRefreshToken(newRefreshToken)
                    }
                    newAccessToken
                } else null
            } else {
                tokenManager.clearTokens()
                null
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            null
        }
    }
}