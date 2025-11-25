package tech.sourceid.sid_address_verification.domain

import android.util.Log
import retrofit2.Response
import tech.sourceid.sid_address_verification.data.requests.AddGeoTagRequest
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.data.responses.AddGeoTagResponse
import tech.sourceid.sid_address_verification.data.responses.CustomerAddressHistoryResponse
import tech.sourceid.sid_address_verification.data.responses.GetOrganisationConfigResponse
import tech.sourceid.sid_address_verification.data.responses.RefreshTokenResponse
import tech.sourceid.sid_address_verification.services.ApiService

class ApiHelper(private val apiService: ApiService) {

    suspend fun fetchOrganisationConfig(
        apiKey: String,
    ): Response<GetOrganisationConfigResponse> {
        Log.d("LocationService", "fetchOrganisationConfig: $apiKey")
        return apiService.fetchOrganisationConfig(apiKey)
    }

    suspend fun fetchCustomerHistory(
        apiKey: String,
        customerID: String,
        verificationGroupId: String,
    ): Response<CustomerAddressHistoryResponse> {

        Log.d("LocationService", "fetchCustomerHistory: verification group id is $verificationGroupId")

        return apiService.fetchCustomerHistory(
            customerID = customerID,
            apiKey = apiKey,
            verificationGroupId = verificationGroupId
        )
    }

    suspend fun addGeoTag(
        apiKey: String,
//        token: String,
//        customerID: String,
        request: AddGeoTagRequest
    ): Response<AddGeoTagResponse> {
        return apiService.addGeoTag(apiKey = apiKey, postBody = request)
    }

    suspend fun refreshToken(
        apiKey: String,
        request: RefreshTokenRequest
    ): Response<RefreshTokenResponse> {
        return apiService.refreshToken(apiKey = apiKey, postBody = request)
    }

    // Add more reusable API calls here...
}
