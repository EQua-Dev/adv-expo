package tech.sourceid.sid_address_verification.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import tech.sourceid.sid_address_verification.data.requests.AddGeoTagRequest
import tech.sourceid.sid_address_verification.data.requests.RefreshTokenRequest
import tech.sourceid.sid_address_verification.data.responses.AddGeoTagResponse
import tech.sourceid.sid_address_verification.data.responses.CustomerAddressHistoryResponse
import tech.sourceid.sid_address_verification.data.responses.GetOrganisationConfigResponse
import tech.sourceid.sid_address_verification.data.responses.RefreshTokenResponse

interface ApiService {


    @GET("organization/address-verification-config")
    suspend fun fetchOrganisationConfig(
//        @Header("x-auth-token") token: String,
        @Header("x-api-key") apiKey: String
    ): Response<GetOrganisationConfigResponse>

    @GET("customer/address-history")
    suspend fun fetchCustomerHistory(
//        @Header("x-auth-token") token: String,
        @Header("customer") customerID: String,
        @Header("x-api-key") apiKey: String,
        @Header("verificationGroupId") verificationGroupId: String,
    ): Response<CustomerAddressHistoryResponse>

    @POST("customer/add-geotag")
    suspend fun addGeoTag(
//        @Header("x-auth-token") token: String,
        @Header("x-api-key") apiKey: String,
        @Body postBody: AddGeoTagRequest
    ): Response<AddGeoTagResponse>

    @POST("auth/customer/refresh-token")
    suspend fun refreshToken(
        @Header("x-api-key") apiKey: String,
        @Body postBody: RefreshTokenRequest
    ): Response<RefreshTokenResponse>
}