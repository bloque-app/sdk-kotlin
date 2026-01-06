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
    val webhookUrl: String? = null
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
    @SerialName("webhook_url") val webhookUrl: String? = null
)

@Serializable
internal data class KycVerificationResponseWire(
    val result: KycVerificationResultWire
)

@Serializable
internal data class KycVerificationResultWire(
    val status: String,
    val url: String,
    @SerialName("completed_at") val completedAt: String? = null
)
