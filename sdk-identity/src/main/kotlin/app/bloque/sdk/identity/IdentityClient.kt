package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Main client for identity operations
 */
class IdentityClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Alias operations
     */
    val aliases: AliasesClient = AliasesClient(httpClient)

    /**
     * Origin operations
     */
    val origins: OriginsClient = OriginsClient(httpClient)
}
