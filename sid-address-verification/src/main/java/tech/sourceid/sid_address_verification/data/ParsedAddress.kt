package tech.sourceid.sid_address_verification.data

data class ParsedAddress(
    val country: String? = null,
    val addressLineOne: String? = null,
    val addressLineTwo: String? = null,
    val city: String? = null,
    val region: String? = null,
    val countryCode: String? = null,
    val postalCode: String? = null,
    val zipCode: String? = null
)
