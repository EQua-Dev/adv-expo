package tech.sourceid.sid_address_verification.data

data class TokenBundle(
    val apiKey: String,
    val accessToken: String,
    val customerID: String,
    val verificationGroupID: String,
)