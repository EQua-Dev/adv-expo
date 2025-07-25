package tech.sourceid.sid_address_verification.domain

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
        return apiService.fetchOrganisationConfig(apiKey)
    }

    suspend fun fetchCustomerHistory(
        apiKey: String,
        token: String,
    ): Response<CustomerAddressHistoryResponse> {
        return apiService.fetchCustomerHistory(token = token, apiKey = apiKey)
    }

    suspend fun addGeoTag(
        apiKey: String,
        token: String,
        request: AddGeoTagRequest
    ): Response<AddGeoTagResponse> {
        return apiService.addGeoTag(token = token, apiKey = apiKey, postBody = request)
    }

    suspend fun refreshToken(
        apiKey: String,
        request: RefreshTokenRequest
    ): Response<RefreshTokenResponse> {
        return apiService.refreshToken(apiKey = apiKey, postBody = request)
    }

    // Add more reusable API calls here...
}
