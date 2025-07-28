package tech.sourceid.sid_address_verification.services.tokenmanager

import android.content.Context
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.domain.ApiHelper
import tech.sourceid.sid_address_verification.services.RetrofitBuilder
import androidx.core.content.edit
import android.util.Log


class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("GeoPrefs", Context.MODE_PRIVATE)
//    private val apiHelper = ApiHelper(RetrofitBuilder.apiService)

    init {
        Log.d("TokenManager", "TokenManager initialized with context: ${context.javaClass.simpleName}")
    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val API_KEY = "api_key"
        private const val CUSTOMER_ID = "customer_id"
    }


    fun saveAccessToken(token: String) {
        Log.d("TokenManager", "About to save access token: $token")
        try {
            val editor = prefs.edit()
            editor.putString(ACCESS_TOKEN, token)
            val success = editor.commit() // Use commit() for immediate write
            Log.d("TokenManager", "Access token save result: $success, token: $token")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving access token", e)
        }
    }


    fun getAccessToken(): String? {
        val token = prefs.getString(ACCESS_TOKEN, null)
        Log.d("TokenManager", "Access token retrieved: $token")
        return token
    }

    fun saveRefreshToken(token: String) {
        Log.d("TokenManager", "About to save refresh token: $token")
        try {
            val editor = prefs.edit()
            editor.putString(REFRESH_TOKEN, token)
            val success = editor.commit()
            Log.d("TokenManager", "Refresh token save result: $success, token: $token")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving refresh token", e)
        }
    }

    fun getRefreshToken(): String? {
        val token = prefs.getString(REFRESH_TOKEN, null)
        Log.d("TokenManager", "Refresh token retrieved: $token")
        return token
    }

    fun saveApiKey(apiKey: String) {
        Log.d("TokenManager", "About to save API key: $apiKey")
        try {
            val editor = prefs.edit()
            editor.putString(API_KEY, apiKey)
            val success = editor.commit()
            Log.d("TokenManager", "API key save result: $success, key: $apiKey")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving API key", e)
        }
    }


    fun getApiKey(): String? {
        val key = prefs.getString(API_KEY, null)
        Log.d("TokenManager", "API key retrieved: $key")
        return key
    }

    fun saveCustomerID(customerID: String) {
        Log.d("TokenManager", "About to save customer ID: $customerID")
        try {
            val editor = prefs.edit()
            editor.putString(CUSTOMER_ID, customerID)
            val success = editor.commit()
            Log.d("TokenManager", "Customer ID save result: $success, ID: $customerID")
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving customer ID", e)
        }
    }
    fun getCustomerID(): String? {
        val id = prefs.getString(CUSTOMER_ID, null)
        Log.d("TokenManager", "Customer ID retrieved: $id")
        return id
    }

    fun clearTokens() {
        prefs.edit {
            remove(ACCESS_TOKEN)
            remove(REFRESH_TOKEN)
        }
        Log.d("TokenManager", "Access and refresh tokens cleared")
    }
    /*suspend fun refreshToken(): String? {
        val refreshToken = getRefreshToken() ?: return null

        return try {
            val refreshTokenRequest = RefreshTokenRequest(refreshToken)

            val response = apiHelper.refreshToken(request = refreshTokenRequest, apiKey = getApiKey()!!)
        *//*    ApiHelper Retrofit . Builder ()
                .baseUrl("https://api.rd.usesourceid.com/v1/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)
                .refreshAccessToken(RefreshTokenRequest(refreshToken))
*//*
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
    }*/
}
