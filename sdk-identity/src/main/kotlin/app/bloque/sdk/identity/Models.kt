package app.bloque.sdk.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================
// Challenge Types
// ============================================

enum class ChallengeType {
    @SerialName("SIGNING_CHALLENGE")
    SIGNING_CHALLENGE,

    @SerialName("API_KEY")
    API_KEY,

    @SerialName("OAUTH_REDIRECT")
    OAUTH_REDIRECT,

    @SerialName("WEBAUTHN")
    WEBAUTHN,

    @SerialName("OTP")
    OTP,

    @SerialName("PASSWORD")
    PASSWORD,

    @SerialName("REDIRECT")
    REDIRECT
}

// ============================================
// Assertion Types
// ============================================

/**
 * Base interface for all assertions
 */
interface OTPAssertion {
    val challengeType: ChallengeType
}

data class OTPAssertionWhatsApp(
    override val challengeType: ChallengeType = ChallengeType.OTP,
    val phoneNumber: String,
    val code: String
) : OTPAssertion

data class OTPAssertionEmail(
    override val challengeType: ChallengeType = ChallengeType.OTP,
    val email: String,
    val code: String
) : OTPAssertion

data class SigningChallengeValue constructor(
    val signature: String,
    val alias: String
)

data class ApiKeyValue constructor(
    val apiKey: String,
    val alias: String
)

// ============================================
// Profile Types
// ============================================

/**
 * User profile for individual accounts (KYC)
 */
data class UserProfile @JvmOverloads constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthdate: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val nationality: String? = null,
    val countryOfResidence: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val documentType: String? = null,
    val documentNumber: String? = null,
    val documentIssueDate: String? = null,
    val documentExpiryDate: String? = null
)

/**
 * Business profile for business accounts (KYB)
 */
data class BusinessProfile @JvmOverloads constructor(
    val legalName: String,
    val taxId: String,
    val incorporationDate: String,
    val businessType: String,
    val incorporationCountryCode: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    val postalCode: String,
    val country: String,
    val website: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val industry: String? = null
)

// ============================================
// Register Parameters
// ============================================

/**
 * Base interface for registration parameters
 */
sealed class RegisterParams {
    abstract val profile: Any
    abstract val metadata: Map<String, String?>?
    var alias: String = ""
    var origin: String = ""

    // Helper method for SDK to set both values
    fun setOriginData(alias: String, origin: String) {
        this.alias = alias
        this.origin = origin
    }
}

data class IndividualRegisterParams @JvmOverloads constructor(
    override val profile: UserProfile,
    override val metadata: Map<String, String?>? = null
) : RegisterParams()

data class BusinessRegisterParams @JvmOverloads constructor(
    override val profile: BusinessProfile,
    override val metadata: Map<String, String?>? = null
) : RegisterParams()

// ============================================
// Wire Types (Internal)
// ============================================

@Serializable
internal data class AssertionResultValueWire(
    @SerialName("api_key") val apiKey: String,
    val alias: String
)

@Serializable
internal data class AssertionResultWireRequest(
    val alias: String,
    val challengeType: String,
    val value: AssertionResultValueWire
)

@Serializable
internal data class RegisterRequestWire(
    @SerialName("assertion_result") val assertionResult: AssertionResultWireRequest,
    @SerialName("extra_context") val extraContext: Map<String, String?> = emptyMap(),
    val type: String,
    val profile: Map<String, String?>
)

@Serializable
internal data class RegisterResponseWire(
    val result: RegisterResultWire
)

@Serializable
internal data class RegisterResultWire(
    val urn: String,
    @SerialName("access_token") val accessToken: String
)

/**
 * Result of registration
 */
data class RegisterResult constructor(
    val urn: String,
    val accessToken: String
)

// ============================================
// Origin Types
// ============================================

@Serializable
data class Origin(
    val name: String,
    val description: String? = null,
    @SerialName("challenge_type") val challengeType: String,
    val enabled: Boolean = true
)

@Serializable
internal data class OriginListResponse(
    val result: OriginListResult
)

@Serializable
internal data class OriginListResult(
    val origins: List<Origin>
)

// ============================================
// Alias Types
// ============================================

@Serializable
data class Alias(
    val alias: String,
    val urn: String,
    val origin: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class AliasResponse(
    val result: AliasResult
)

@Serializable
internal data class AliasResult(
    val alias: Alias
)

// ============================================
// Assertion Types (Wire)
// ============================================

@Serializable
internal data class AssertionResponseWire(
    val result: AssertionResultWire
)

@Serializable
internal data class AssertionResultWire(
    @SerialName("challenge_type") val challengeType: String,
    val value: Map<String, String>
)

/**
 * Result of assertion request
 */
data class AssertionResult constructor(
    val challengeType: ChallengeType,
    val value: Map<String, String>
)
