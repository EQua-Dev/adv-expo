package tech.sourceid.sid_address_verification.services.tokenmanager

import android.content.Context
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.domain.ApiHelper
import tech.sourceid.sid_address_verification.services.RetrofitBuilder
import androidx.core.content.edit

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val apiHelper = ApiHelper(RetrofitBuilder.apiService)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val API_KEY = "api_key"
        private const val CUSTOMER_ID = "customer_id"
    }

    fun saveAccessToken(token: String) {
        prefs.edit { putString(ACCESS_TOKEN, token) }
    }

    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit { putString(REFRESH_TOKEN, token) }
    }

    fun getRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN, null)
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit { putString(API_KEY, apiKey) }
    }

    fun getApiKey(): String? {
        return prefs.getString(API_KEY, null)
    }

    fun saveCustomerID(apiKey: String) {
        prefs.edit { putString(CUSTOMER_ID, apiKey) }
    }

    fun getCustomerID(): String? {
        return prefs.getString(CUSTOMER_ID, null)
    }

    suspend fun refreshToken(): String? {
        val refreshToken = getRefreshToken() ?: return null

        return try {
            val refreshTokenRequest = RefreshTokenRequest(refreshToken)

            val response = apiHelper.refreshToken(request = refreshTokenRequest, apiKey = getApiKey()!!)
        /*    ApiHelper Retrofit . Builder ()
                .baseUrl("https://api.rd.usesourceid.com/v1/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)
                .refreshAccessToken(RefreshTokenRequest(refreshToken))
*/
            if (response.isSuccessful) {
                val newAccessToken = response.body()?.data!!.accessToken
                run {
                    saveAccessToken(newAccessToken)
                    newAccessToken
                }
            } else null

        } catch (e: Exception) {
            null
        }
    }
}
