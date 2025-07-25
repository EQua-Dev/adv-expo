package tech.sourceid.sid_address_verification.data.responses

data class RefreshTokenResponse(
    val `data`: RefreshTokenData,
    val message: String,
    val status: Boolean,
    val statusCode: Int
)

data class RefreshTokenData(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String
)