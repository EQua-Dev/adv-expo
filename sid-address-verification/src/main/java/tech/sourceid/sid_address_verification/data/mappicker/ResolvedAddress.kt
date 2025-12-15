package tech.sourceid.sid_address_verification.data.mappicker

data class ResolvedAddress(
    val latitude: Double,
    val longitude: Double,
    val fullAddress: String,
    val country: String?,
    val state: String?,
    val city: String?,
    val postalCode: String?,
    val street: String?
)
