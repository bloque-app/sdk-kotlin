package app.bloque.sdk

import app.bloque.sdk.accounts.AccountsClient
import app.bloque.sdk.compliance.ComplianceClient
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.identity.IdentityClient
import app.bloque.sdk.orgs.OrgsClient

/**
 * User session after successful connection
 * Provides access to all SDK modules
 */
class UserSession internal constructor(
    private val httpClient: BloqueHttpClient
) {
    /**
     * Account operations (bancolombia, card, virtual, polygon)
     */
    val accounts: AccountsClient = AccountsClient(httpClient)

    /**
     * Identity operations (aliases, origins)
     */
    val identity: IdentityClient = IdentityClient(httpClient)

    /**
     * Compliance operations (KYC)
     */
    val compliance: ComplianceClient = ComplianceClient(httpClient)

    /**
     * Organization operations
     */
    val orgs: OrgsClient = OrgsClient(httpClient)

    /**
     * Get the current user URN
     */
    fun getUrn(): String? = httpClient.getUrn()
}
