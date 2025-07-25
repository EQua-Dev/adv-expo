package tech.sourceid.sid_address_verification.services.tokenmanager

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager // Your class that handles storing/fetching tokens
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite loop
        if (responseCount(response) >= 2) return null

        // Refresh token synchronously
        val newToken = tokenManager.getRefreshToken() ?: return null

        // Save new token
        tokenManager.saveAccessToken(newToken)

        // Retry the request with new token
        return response.request.newBuilder()
            .header("x-auth-token", newToken)
            .build()
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
