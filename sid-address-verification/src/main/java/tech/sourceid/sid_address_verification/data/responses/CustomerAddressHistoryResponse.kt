package tech.sourceid.sid_address_verification.data.responses

//@Serialzable
data class CustomerAddressHistoryResponse(
    val `data`: List<Data>,
    val message: String,
    val status: Boolean,
    val statusCode: Int
)

data class Data(
    val _id: String,
    val artifact: String,
    val customer: String,
    val metadata: Metadata,
    val organization: String,
    val reference: String,
    val verification: String,
    val verificationRequestPayload: String,
    val verificationResponse: Any,
    val verificationStatus: String,
    val verifiedAt: String
)

data class Metadata(
    val addressLineOne: String,
    val addressType: String,
    val latitude: Double,
    val locations: List<Location>,
    val longitude: Double,
    val verificationEndDate: String
)

data class Location(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)