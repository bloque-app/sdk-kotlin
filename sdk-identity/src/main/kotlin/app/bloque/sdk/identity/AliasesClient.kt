package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for alias operations
 */
class AliasesClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Get information about an alias
     *
     * @param alias The alias to query
     * @return Alias information
     */
    fun get(alias: String): Alias {
        val response = httpClient.get<AliasResponse>(
            path = "/api/aliases/$alias"
        )

        return response.result.alias
    }
}
