package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Main client for compliance operations
 */
class ComplianceClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * KYC (Know Your Customer) operations
     */
    val kyc: KycClient = KycClient(httpClient)
}
