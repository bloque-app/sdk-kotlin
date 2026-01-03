package app.bloque.sdk

import app.bloque.sdk.accounts.AccountsClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * User session after successful connection
 * Provides access to all SDK modules
 */
class UserSession internal constructor(
    private val httpClient: BloqueHttpClient
) {
    /**
     * Account operations (bancolombia, etc.)
     */
    val accounts: AccountsClient = AccountsClient(httpClient)
}
