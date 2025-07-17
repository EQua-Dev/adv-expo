package tech.sourceid.sid_address_verification.data.responses

data class GetOrganisationConfigResponse(
    val `data`: OrganisationConfigData,
    val message: String,
    val status: Boolean,
    val statusCode: Int
)

data class OrganisationConfigData(
    val distanceTolerance: Int,
    val geotaggingPollingInterval: Double,
    val geotaggingSessionTimeout: Int
)