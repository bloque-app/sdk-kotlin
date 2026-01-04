package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for KYC (Know Your Customer) operations
 */
class KycClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Start a KYC verification process
     *
     * @param params Verification parameters with URN and optional webhook URL
     * @return KYC verification response with status and verification URL
     */
    fun startVerification(params: KycVerificationParams): KycVerificationResponse {
        val request = KycVerificationRequestWire(
            urn = params.urn,
            webhookUrl = params.webhookUrl
        )

        val response = httpClient.post<KycVerificationResponseWire, KycVerificationRequestWire>(
            path = "/api/compliance/kyc/start",
            body = request
        )

        return mapResponse(response.result)
    }

    /**
     * Get the status of a KYC verification
     *
     * @param params Parameters with URN
     * @return KYC verification response with current status
     */
    fun getVerification(params: GetKycVerificationParams): KycVerificationResponse {
        val response = httpClient.get<KycVerificationResponseWire>(
            path = "/api/compliance/kyc/${params.urn}"
        )

        return mapResponse(response.result)
    }

    private fun mapResponse(wire: KycVerificationResultWire): KycVerificationResponse {
        val status = when (wire.status) {
            "awaiting_compliance_verification" -> KycVerificationStatus.AWAITING_VERIFICATION
            "approved" -> KycVerificationStatus.APPROVED
            "rejected" -> KycVerificationStatus.REJECTED
            else -> KycVerificationStatus.AWAITING_VERIFICATION
        }

        return KycVerificationResponse(
            status = status,
            url = wire.url,
            completedAt = wire.completedAt
        )
    }
}
