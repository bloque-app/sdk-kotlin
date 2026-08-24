package app.bloque.sdk.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

/**
 * A challenge returned by [OriginClient.attest] (registration) or
 * [OriginClient.assert] (authentication) — decoded straight off the wire,
 * with no envelope. [value] and [params] are free-form and shaped
 * differently per [type] (e.g. `{challenge, timestamp}` for a
 * `SIGNING_CHALLENGE`, an OTP delivery confirmation for `OTP`), so they are
 * kept as raw [JsonElement]/[JsonObject] rather than a fixed schema.
 */
@Serializable
data class Challenge(
    val type: ChallengeType,
    val value: JsonElement,
    val params: JsonObject? = null
)

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

/**
 * Deprecated, incorrect shape for the origin `assert`/`attest` response —
 * it invented a `challenge_type` wire key (the real key is `type`) and
 * assumed `value` was always a flat `Map<String, String>`, which does not
 * hold for challenges like `SIGNING_CHALLENGE` (`{challenge, timestamp}`,
 * with a numeric timestamp). Replaced by [Challenge], which decodes the
 * real wire shape.
 */
@Deprecated(
    "Replaced by Challenge, which matches the real /assert and /attest wire shape",
    ReplaceWith("Challenge")
)
data class AssertionResult constructor(
    val challengeType: ChallengeType,
    val value: Map<String, String>
)

// ============================================
// Connect Types
// ============================================

/**
 * Parameters to resolve an assertion challenge (from [OriginClient.assert])
 * and finish connecting to an existing identity via [OriginClient.connect].
 * Covers any challenge type — an OTP code, a signature, an API key, etc. —
 * not just the legacy API_KEY flow that [app.bloque.sdk.BloqueSDK.connect]
 * handles directly.
 */
data class ConnectParams @JvmOverloads constructor(
    val challengeType: ChallengeType,
    val value: Map<String, String>,
    val alias: String? = null,
    val originalChallengeParams: Map<String, String>? = null,
    val metadata: Map<String, String?>? = null
)

/**
 * Result of [OriginClient.connect] — an access token for the now-connected
 * identity.
 */
data class ConnectResult constructor(
    val accessToken: String,
    val reqId: String? = null
)

@Serializable
internal data class GenericAssertionResultWire(
    val alias: String? = null,
    val challengeType: String,
    val value: Map<String, String>,
    val originalChallengeParams: Map<String, String>? = null
)

@Serializable
internal data class ConnectRequestWire(
    @SerialName("assertion_result") val assertionResult: GenericAssertionResultWire,
    @SerialName("extra_context") val extraContext: Map<String, String?> = emptyMap()
)

@Serializable
internal data class ConnectResultWire(
    @SerialName("access_token") val accessToken: String
)

@Serializable
internal data class ConnectResponseWire(
    val result: ConnectResultWire,
    @SerialName("req_id") val reqId: String? = null
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
    val countryOfResidence: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val documentType: String? = null,
    val documentNumber: String? = null
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
 * Business profile for business accounts (KYB), including the UBO
 * (ultimate beneficial owner) fields the domain `Business` type carries.
 */
data class BusinessProfile @JvmOverloads constructor(
    var name: String,
    val legalName: String,
    val taxId: String,
    val incorporationDate: String,
    val businessType: String,
    val countryCode: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val website: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val industry: String? = null,
    val ownerName: String? = null,
    val ownerIdType: String? = null,
    val ownerIdNumber: String? = null,
    val ownerAddressLine1: String? = null,
    val ownerAddressLine2: String? = null,
    val ownerCity: String? = null,
    val ownerState: String? = null,
    val ownerPostalCode: String? = null,
    val ownerCountryCode: String? = null
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

/**
 * An origin as returned by the public, unauthenticated `GET /origins`
 * listing. `metadata` is always `{}` here by design — it is a free-form
 * blob that has held secrets and commercially sensitive data in
 * production, so the API deliberately never exposes it on this anonymous
 * endpoint. Origin-specific presentation fields (`company`,
 * `gate_accent_color`, ...) are resolved through their own narrow,
 * purpose-built flows instead (see the hosted TOS/verification gates).
 */
@Serializable
data class Origin(
    val namespace: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val metadata: Map<String, JsonElement> = emptyMap()
)

// ============================================
// Alias Types
// ============================================

enum class AliasType {
    @SerialName("email")
    EMAIL,

    @SerialName("phone")
    PHONE,

    @SerialName("username")
    USERNAME,

    @SerialName("other")
    OTHER,

    @SerialName("wallet")
    WALLET
}

enum class AliasStatus {
    @SerialName("awaiting_verification")
    AWAITING_VERIFICATION,

    @SerialName("active")
    ACTIVE,

    @SerialName("inactive")
    INACTIVE,

    @SerialName("blocked")
    BLOCKED,

    @SerialName("rejected")
    REJECTED
}

/**
 * An identity alias (`IdAlias` on the wire) — decoded raw, with no
 * envelope, from `GET /aliases?alias=`, `GET /identities/me/aliases`, and
 * `GET /identities/:urn/aliases`. `details` shape depends on [type] (e.g.
 * `{email: "..."}`, `{phone: "..."}`), so it is kept as a raw [JsonElement].
 */
@Serializable
data class Alias(
    val alias: String,
    val type: AliasType,
    val urn: String,
    val origin: String,
    val status: AliasStatus,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("is_primary") val isPrimary: Boolean,
    val details: JsonElement? = null
)

/**
 * Result of [AliasesClient.verify] — the JSON confirmation path of
 * `GET /aliases/verify?token=...` (no `redirect_uri`). The `redirect_uri`
 * variant of that endpoint is a browser-navigation feature (a 301 redirect
 * to a hosted page) and isn't modeled as an SDK call — following it through
 * this HTTP client would return an arbitrary non-JSON page, not data your
 * code can use. Build the URL directly and open it in a browser/web view
 * if you need that flow.
 */
@Serializable
data class VerifyAliasResult(
    val success: Boolean,
    val alias: String
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
    val scopes: List<String>? = null
)

@Serializable
data class ExchangeApiKeyResult(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

/**
 * Result of [ApiKeysClient.rotate]. The API's `RotateResult` only ever
 * carries the new [secretKey] — `key_id`/`publishable_key` don't change on
 * rotation and were never part of this response, so they aren't modeled
 * here.
 */
@Serializable
data class RotateApiKeyResult(
    @SerialName("secret_key") val secretKey: String
)

// ============================================
// Identity Types
// ============================================

enum class IdentityStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("inactive")
    INACTIVE,

    @SerialName("blocked")
    BLOCKED,

    @SerialName("awaiting_compliance_verification")
    AWAITING_COMPLIANCE_VERIFICATION,

    @SerialName("compliance_rejected")
    COMPLIANCE_REJECTED,

    @SerialName("compliance_fatal_rejected")
    COMPLIANCE_FATAL_REJECTED
}

/**
 * An identity, decoded raw off the wire (no envelope) from
 * `GET /identities/me` and `GET /identities/:urn`, and unwrapped from
 * `{result: {identity}}` for `PATCH /identities/me` and
 * `PATCH /identities/:urn`.
 */
@Serializable
data class IdentityMe(
    val urn: String,
    val origin: String,
    val type: String? = null,
    val profile: Map<String, String?> = emptyMap(),
    val status: IdentityStatus? = null,
    val metadata: Map<String, JsonElement> = emptyMap()
)

/**
 * Parameters for [IdentityClient.updateMe] / [IdentityClient.update] — only
 * `profile` and `metadata` are updatable, and each is shallow-merged
 * server-side into the existing values (omit a field here to leave it
 * untouched; it is not cleared).
 */
data class UpdateIdentityParams @JvmOverloads constructor(
    val profile: Map<String, String?>? = null,
    val metadata: Map<String, Any?>? = null
)

@Serializable
internal data class UpdateIdentityRequestWire(
    val profile: Map<String, String?>? = null,
    val metadata: JsonElement? = null
)

@Serializable
internal data class UpdateIdentityResultWire(
    val identity: IdentityMe
)

@Serializable
internal data class UpdateIdentityResponseWire(
    val result: UpdateIdentityResultWire,
    @SerialName("req_id") val reqId: String? = null
)

// ============================================
// API Key Wire Types (Internal)
// ============================================

@Serializable
internal data class CreateApiKeyRequestWire(
    val name: String,
    val scopes: List<String>,
    val domains: List<String>,
    val expiration: String,
    val metadata: Map<String, String>? = null
)

@Serializable
internal data class ExchangeApiKeyRequestWire(
    val key: String,
    val scopes: List<String>? = null
)

/**
 * Decodes either a real `ApiKeyInfo` or the API's not-quite-a-404: `GET
 * /api-keys/:id` returns HTTP 200 with `{statusCode: 404, message: ...}`
 * when the key doesn't exist, instead of a real 404 status
 * (`api-key.controller.ts:305`). [keyId] is `null` on that error shape (and
 * always present otherwise), so it's the discriminator [ApiKeysClient.get]
 * uses to decide whether to throw.
 */
@Serializable
internal data class ApiKeyOrErrorWire(
    val id: String? = null,
    @SerialName("key_id") val keyId: String? = null,
    @SerialName("publishable_key") val publishableKey: String? = null,
    val name: String? = null,
    val scopes: List<String>? = null,
    val domains: List<String>? = null,
    val status: String? = null,
    val expiration: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val statusCode: Int? = null,
    val message: String? = null
)

@Serializable
internal data class MessageResponseWire(
    val message: String
)
