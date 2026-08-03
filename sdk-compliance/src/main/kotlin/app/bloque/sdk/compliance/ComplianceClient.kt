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

    /**
     * Read an identity's effective tier and requirement status.
     */
    val tiers: TiersClient = TiersClient(httpClient)

    /**
     * Level 0 TOS gate — hosted acceptance flow.
     */
    val tosGate: TosGateClient = TosGateClient(httpClient)

    /**
     * Phase 3 hosted verification gate — document/form submission flow.
     */
    val verificationGate: VerificationGateClient = VerificationGateClient(httpClient)
}
