package app.bloque.sdk.identity

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
 * Basic user profile for minimal registration (only phone required)
 */
data class BasicUserProfile(
    val phoneNumber: String
)

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
 * Business type enumeration
 */
enum class BusinessType {
    SAS,      // Sociedad por Acciones Simplificada
    SA,       // Sociedad Anónima
    LTDA,     // Sociedad Limitada
    SL,       // Sociedad Limitada (Spain)
    LLC,      // Limited Liability Company
    CORP,     // Corporation
    OTHER
}

/**
 * Basic business profile for minimal registration
 */
data class BasicBusinessProfile @JvmOverloads constructor(
    val name: String,
    val legalName: String,
    val taxId: String,
    val businessType: BusinessType,
    val email: String,
    val incorporationDate: String,
    val country: String,
    val phone: String? = null
)

/**
 * Business profile for business accounts (KYB)
 */
data class BusinessProfile @JvmOverloads constructor(
    var name: String,
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
    abstract val idempotencyKey: String?
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
    override val metadata: Map<String, String?>? = null,
    override val idempotencyKey: String? = null
) : RegisterParams()

data class BasicIndividualRegisterParams @JvmOverloads constructor(
    override val profile: BasicUserProfile,
    override val metadata: Map<String, String?>? = null,
    override val idempotencyKey: String? = null
) : RegisterParams()

data class BusinessRegisterParams @JvmOverloads constructor(
    override val profile: BusinessProfile,
    override val metadata: Map<String, String?>? = null,
    override val idempotencyKey: String? = null
) : RegisterParams()

data class BasicBusinessRegisterParams @JvmOverloads constructor(
    override val profile: BasicBusinessProfile,
    override val metadata: Map<String, String?>? = null,
    override val idempotencyKey: String? = null
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

// ============================================
// API Key Types
// ============================================

@Serializable
data class ApiKeyInfo(
    val id: String,
    @SerialName("key_id") val keyId: String,
    @SerialName("publishable_key") val publishableKey: String,
    val name: String,
    val scopes: List<String>,
    val domains: List<String>,
    val status: String,
    val expiration: String,
    val metadata: Map<String, String> = emptyMap(),
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

data class CreateApiKeyParams @JvmOverloads constructor(
    val name: String,
    val scopes: List<String>,
    val domains: List<String>,
    val expiration: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class CreateApiKeyResult(
    @SerialName("key_id") val keyId: String,
    @SerialName("secret_key") val secretKey: String,
    @SerialName("publishable_key") val publishableKey: String
)

data class ExchangeApiKeyParams @JvmOverloads constructor(
    val key: String,
    val scopes: List<String>? = null,
    /**
     * Origin-bound keys only. Issues an owner-read impersonation JWT
     * (`kind: api-key`, `sub` = that identity). Requires the session to
     * already hold a `kind: origin-operator` Bearer (call
     * `orgs.assumeOrigin` first). Cross-origin URNs return 404; unbound
     * keys return 400 `E_AS_IDENTITY_NOT_ALLOWED`.
     */
    val asIdentity: String? = null
)

@Serializable
data class ExchangeApiKeyResult(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class RotateApiKeyResult(
    @SerialName("key_id") val keyId: String,
    @SerialName("secret_key") val secretKey: String,
    @SerialName("publishable_key") val publishableKey: String
)

// ============================================
// Identity Me Types
// ============================================

@Serializable
data class IdentityMe(
    val urn: String,
    val origin: String,
    val type: String? = null,
    val profile: Map<String, String?> = emptyMap()
)

// ============================================
// API Key Wire Types (Internal)
// ============================================

@Serializable
internal data class ApiKeyListResponseWire(
    val result: List<ApiKeyInfo>
)

@Serializable
internal data class ApiKeyResponseWire(
    val result: ApiKeyInfo
)

@Serializable
internal data class CreateApiKeyRequestWire(
    val name: String,
    val scopes: List<String>,
    val domains: List<String>,
    val expiration: String,
    val metadata: Map<String, String>? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class ExchangeApiKeyRequestWire(
    val key: String,
    val scopes: List<String>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("as_identity")
    val asIdentity: String? = null
)

@Serializable
internal data class RotateApiKeyResponseWire(
    val result: RotateApiKeyResult
)

@Serializable
internal data class IdentityMeResponseWire(
    val result: IdentityMe
)

@Serializable
internal data class MessageResponseWire(
    val message: String
)

