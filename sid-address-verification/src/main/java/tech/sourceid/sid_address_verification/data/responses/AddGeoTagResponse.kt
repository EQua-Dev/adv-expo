package tech.sourceid.sid_address_verification.data.responses

data class AddGeoTagResponse(
    val `data`: Int,
    val message: String,
    val status: Boolean,
    val statusCode: Int
)