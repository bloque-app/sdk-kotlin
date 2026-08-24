package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import java.net.URLEncoder

/**
 * Client for alias operations
 */
class AliasesClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Find an alias by its value
     *
     * @param alias The alias value to look up
     * @return Alias information
     */
    fun get(alias: String): Alias {
        return httpClient.get<Alias>(path = "/api/aliases?alias=${URLEncoder.encode(alias, "UTF-8")}")
    }

    /**
     * List the caller's own aliases
     *
     * @return All aliases belonging to the currently authenticated identity
     */
    fun mine(): List<Alias> {
        return httpClient.get<List<Alias>>(path = "/api/identities/me/aliases")
    }

    /**
     * List the aliases belonging to another identity. Access is subject to
     * policy evaluation.
     *
     * @param urn The URN of the identity whose aliases to retrieve
     * @return All aliases belonging to that identity
     */
    fun forIdentity(urn: String): List<Alias> {
        return httpClient.get<List<Alias>>(path = "/api/identities/$urn/aliases")
    }

    /**
     * Verify an alias using the verification token it was sent (e.g. by
     * email/SMS). Only the JSON confirmation path is modeled here — see
     * [VerifyAliasResult] for why the `redirect_uri` variant isn't.
     *
     * @param token The verification token
     * @return Whether verification succeeded, and which alias was verified
     */
    fun verify(token: String): VerifyAliasResult {
        return httpClient.get<VerifyAliasResult>(
            path = "/api/aliases/verify?token=${URLEncoder.encode(token, "UTF-8")}"
        )
    }
}
