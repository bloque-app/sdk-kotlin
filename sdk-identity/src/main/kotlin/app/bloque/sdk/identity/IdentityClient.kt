package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Main client for identity operations
 */
class IdentityClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    val aliases: AliasesClient = AliasesClient(httpClient)
    val origins: OriginsClient = OriginsClient(httpClient)
    val apiKeys: ApiKeysClient = ApiKeysClient(httpClient)

    /**
     * Retrieve the authenticated identity's own profile.
     * Used by ApiKey auth to discover origin and urn after exchange.
     */
    fun me(): IdentityMe {
        val response = httpClient.get<IdentityMeResponseWire>(path = "/api/identities/me")
        return response.result
    }
}
