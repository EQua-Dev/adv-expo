package tech.sourceid.sid_address_verification.data.requests

data class AddGeoTagRequest(
    val address: String,
    val latitude: Double,
    val longitude: Double
)