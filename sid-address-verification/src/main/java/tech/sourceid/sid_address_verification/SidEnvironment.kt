package tech.sourceid.sid_address_verification

/**
 * Single source of truth for the SourceID gateway base URL.
 *
 * The environment is derived from the API key prefix, so consumers never pass
 * a URL. Every network call in this SDK must resolve its base URL here —
 * hardcoding a host at the call site is what previously pinned parts of the
 * SDK to a decommissioned server.
 *
 * UAT is intentionally absent: that environment was retired along with the
 * `usesourceid.com` hosts.
 */
internal object SidEnvironment {

    private const val PRODUCTION = "https://api.sourceid.tech/v1/api/"
    private const val SANDBOX = "https://api.sbx.sourceid.tech/v1/api/"
    private const val DEVELOPMENT = "https://api-rd.tailfaed50.ts.net/v1/api/"

    /**
     * @throws IllegalArgumentException when the key does not name a known
     * environment.
     */
    fun resolveBaseUrl(apiKey: String): String = when {
        apiKey.startsWith("sk_live_") -> PRODUCTION
        apiKey.startsWith("sk_sbx_") -> SANDBOX
        apiKey.startsWith("sk_rd_") -> DEVELOPMENT
        else -> throw IllegalArgumentException(
            "Invalid API key: unknown environment. Expected a key beginning " +
                "with sk_live_, sk_sbx_, or sk_rd_."
        )
    }
}
