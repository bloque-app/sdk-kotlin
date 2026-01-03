package app.bloque.sdk.accounts

import app.bloque.sdk.core.BloqueHttpClient

/**
 * Main client for all account operations
 */
class AccountsClient(
    httpClient: BloqueHttpClient
) {
    /**
     * Bancolombia account operations
     */
    val bancolombia: BancolombiaClient = BancolombiaClient(httpClient)
}
