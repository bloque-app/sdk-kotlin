package app.bloque.sdk.compliance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================
// KYC Verification Types
// ============================================

/**
 * KYC verification status
 */
enum class KycVerificationStatus {
    @SerialName("awaiting_compliance_verification")
    AWAITING_VERIFICATION,

    @SerialName("approved")
    APPROVED,

    @SerialName("rejected")
    REJECTED
}

/**
 * Parameters for starting KYC verification
 */
data class KycVerificationParams @JvmOverloads constructor(
    val urn: String,
    val type: String = "kyc",
    val accompliceType: String = "person",
    val webhookUrl: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Parameters for getting KYC verification status
 */
data class GetKycVerificationParams constructor(
    val urn: String
)

/**
 * KYC verification response
 */
data class KycVerificationResponse @JvmOverloads constructor(
    val status: KycVerificationStatus,
    val url: String,
    val completedAt: String? = null
)

// ============================================
// Wire Types (Internal)
// ============================================

@Serializable
internal data class KycVerificationRequestWire(
    val urn: String,
    val type: String = "kyc",
    @SerialName("accomplice_type") val accompliceType: String = "person",
    @SerialName("webhook_url") val webhookUrl: String? = null
)

/** Direct API response (no result wrapper) for start and get */
@Serializable
internal data class KycVerificationResponseDirect(
    val status: String,
    val url: String? = null,
    @SerialName("verification_url") val verificationUrl: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val type: String? = null,
    val level: String? = null,
    val provider: String? = null
)
