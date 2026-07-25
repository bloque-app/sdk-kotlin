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
            type = params.type,
            accompliceType = params.accompliceType,
            webhookUrl = params.webhookUrl
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<KycVerificationResponseDirect, KycVerificationRequestWire>(
            path = "/api/compliance",
            body = request,
            headers = headers
        )

        return mapStartResponse(response)
    }

    /**
     * Get the status of a KYC verification
     *
     * @param params Parameters with URN
     * @return KYC verification response with current status
     */
    fun getVerification(params: GetKycVerificationParams): KycVerificationResponse {
        val response = httpClient.get<KycVerificationResponseDirect>(
            path = "/api/compliance/${params.urn}"
        )

        return mapGetResponse(response)
    }

    private fun mapStartResponse(wire: KycVerificationResponseDirect): KycVerificationResponse {
        val status = when (wire.status) {
            "awaiting_compliance_verification" -> KycVerificationStatus.AWAITING_VERIFICATION
            "approved" -> KycVerificationStatus.APPROVED
            "rejected" -> KycVerificationStatus.REJECTED
            else -> KycVerificationStatus.AWAITING_VERIFICATION
        }

        return KycVerificationResponse(
            status = status,
            url = wire.url ?: "",
            completedAt = null
        )
    }

    private fun mapGetResponse(wire: KycVerificationResponseDirect): KycVerificationResponse {
        val status = when (wire.status) {
            "awaiting_compliance_verification" -> KycVerificationStatus.AWAITING_VERIFICATION
            "approved" -> KycVerificationStatus.APPROVED
            "rejected" -> KycVerificationStatus.REJECTED
            else -> KycVerificationStatus.AWAITING_VERIFICATION
        }

        return KycVerificationResponse(
            status = status,
            url = wire.verificationUrl ?: wire.url ?: "",
            completedAt = wire.completedAt
        )
    }
}
