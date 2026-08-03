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
// Tier Status Types
// ============================================

/**
 * Status of a single requirement's evidence.
 *
 * `PENDING_REVIEW` still blocks the level, but the customer has already
 * submitted it — show it as in progress rather than asking again.
 */
enum class RequirementEvidenceStatus {
    @SerialName("satisfied")
    SATISFIED,

    @SerialName("not_satisfied")
    NOT_SATISFIED,

    @SerialName("expired")
    EXPIRED,

    @SerialName("revoked")
    REVOKED,

    @SerialName("pending_review")
    PENDING_REVIEW
}

/** A form field a `document`/`manual_review` requirement may ask for. */
enum class RequirementFieldType {
    TEXT, NUMBER, DATE, SELECT, BOOLEAN
}

/**
 * A single field definition for a requirement that collects form answers.
 */
data class RequirementField @JvmOverloads constructor(
    val key: String,
    val label: String,
    val type: RequirementFieldType,
    val required: Boolean? = null,
    /** Only meaningful for [RequirementFieldType.SELECT]. */
    val options: List<String>? = null
)

/** Status of a single requirement within a tier level. */
data class TierRequirementStatus @JvmOverloads constructor(
    val key: String,
    /** e.g. `"tos"`, `"kyc"`, `"document"`, `"manual_review"`, `"provider_check"`. */
    val kind: String,
    val status: RequirementEvidenceStatus,
    /** What this requirement means, for display without hardcoding key strings. */
    val description: String? = null,
    /** Only present for requirements that collect form answers. */
    val fields: List<RequirementField>? = null,
    /** ISO-8601 timestamp of the submission behind a `PENDING_REVIEW` status. */
    val submittedAt: String? = null
)

/** Status of a single tier level, including all of its requirements. */
data class TierLevelStatus(
    val level: Int,
    val name: String,
    val satisfied: Boolean,
    val requirements: List<TierRequirementStatus>
)

/** Which kind of hosted gate resolves an outstanding verification gap. */
enum class VerificationFlowType {
    TOS_HOSTED_ACCEPTANCE, DOCUMENT_SUBMISSION
}

/**
 * Machine-readable pointer to the hosted gate that resolves the caller's
 * current verification gap. Feed [startEndpoint] into a direct request, or
 * just catch the thrown verification error and call its
 * `getVerificationLink()` instead.
 */
data class VerificationFlowHandoff(
    val type: VerificationFlowType,
    val method: String,
    val startEndpoint: String,
    val responseUrlField: String
)

/** Parameters for reading an identity's compliance tier status. */
data class GetTierStatusParams(val urn: String)

/**
 * An identity's effective compliance tier, per-level requirement status,
 * and (if not fully verified) which requirements are missing and which
 * hosted gate resolves them.
 */
data class TierStatus @JvmOverloads constructor(
    val identityUrn: String,
    /** Highest contiguous satisfied level; -1 if even Level 0 is unsatisfied. */
    val effectiveLevel: Int,
    val policyVersion: String,
    val levels: List<TierLevelStatus>,
    /** The next level that isn't fully satisfied, if any. */
    val nextLevel: Int? = null,
    /** Requirement keys still outstanding at [nextLevel], actionable or not. */
    val missingRequirements: List<String> = emptyList(),
    /**
     * The subset of [missingRequirements] already submitted and awaiting a
     * reviewer. When it covers all of them, [verificationFlow] is absent:
     * there is nothing left for the customer to do.
     */
    val pendingRequirements: List<String> = emptyList(),
    val verificationFlow: VerificationFlowHandoff? = null
)

// ============================================
// Hosted Gates — Shared Types
// ============================================

/**
 * Result of starting a hosted gate (`tosGate.start` / `verificationGate.start`)
 * — both `/start` endpoints share this exact response shape.
 */
data class StartGateResult(
    /** Capability token — pass to `init()`/`accept()`/`submit()`. */
    val token: String,
    /** Fully-qualified hosted page URL a browser should open. */
    val url: String,
    /** Token lifetime, e.g. `"30m"`. */
    val expiresIn: String
)

// ============================================
// TOS Gate Types
// ============================================

/** Parameters for starting the Level 0 TOS gate. */
data class StartTosGateParams(
    /**
     * Where the hosted TOS gate page redirects back to after acceptance.
     * Must be present in the backend's return-URL allowlist.
     */
    val returnUrl: String
)

/** Parameters shared by [TosGateClient.init] and [TosGateClient.accept]. */
data class TosGateInitParams(val token: String)

data class TosGateDocument(
    val documentVersionId: String,
    val versionLabel: String,
    val contentHash: String,
    /** Rendered document content (Markdown-templated to HTML server-side). */
    val content: String
)

data class TosGateInitResult(
    val document: TosGateDocument,
    /** Single-use acceptance nonce — pass to `accept()`. */
    val csrfToken: String,
    val returnUrl: String,
    /** Whether the hosted page's intro screens should play before the document. */
    val showHome: Boolean
)

data class TosGateAcceptParams @JvmOverloads constructor(
    /** The capability token returned by `start()`. */
    val token: String,
    /** The single-use nonce returned by `init()`. */
    val csrfToken: String,
    /**
     * Optional Pass device attestation (0x-hex SCALE-encoded
     * `PassDeviceAttestation`). When present, accepting the terms also hands
     * control of the identity's Kreivo PassAccount to that device.
     */
    val deviceAttestation: String? = null
)

data class TosAcceptanceRecord(
    val id: String,
    val identityUrn: String,
    val documentVersionId: String,
    val documentVersionLabel: String,
    val documentHash: String,
    val acceptedAt: String,
    val authAssurance: String
)

data class TosGateAcceptResult(
    val acceptance: TosAcceptanceRecord,
    val returnUrl: String
)

// ============================================
// Verification Gate Types
// ============================================

/** Parameters for starting the hosted verification gate. */
data class StartVerificationGateParams @JvmOverloads constructor(
    /**
     * Where the hosted verification gate page redirects back to after
     * submission. Optional — when omitted, the hosted page does not offer
     * a way back to the caller's app.
     *
     * Validated fail-closed against the union of two allowlists: the
     * calling origin's own `metadata.verification_gate_return_url_allowlist`
     * (if configured) and the deployment-wide
     * `VERIFICATION_GATE_RETURN_URL_ALLOWLIST` env var. Either one being
     * satisfied is enough.
     */
    val returnUrl: String? = null
)

data class VerificationGateInitParams(val token: String)

data class VerificationUploadIntent(
    val contentType: String,
    /** The S3 key to confirm in `submit()` once the PUT completes. */
    val key: String,
    /** Presigned URL for a direct browser PUT upload. */
    val uploadUrl: String,
    val maxSizeBytes: Long
)

data class VerificationRequirement @JvmOverloads constructor(
    val key: String,
    /** e.g. `"document"` or `"manual_review"` — TOS/KYC never appear here. */
    val kind: String,
    val description: String? = null,
    /** Only present for requirements that collect form answers. */
    val fields: List<RequirementField>? = null,
    val uploadable: Boolean,
    /** One presigned upload URL per allowed content type, only when [uploadable]. */
    val uploadIntents: List<VerificationUploadIntent>? = null
)

/**
 * A requirement already submitted and waiting on a reviewer. Nothing to
 * collect, only something to report.
 */
data class PendingVerificationRequirement @JvmOverloads constructor(
    val key: String,
    val description: String? = null,
    /** ISO-8601 timestamp of the submission. */
    val submittedAt: String? = null
)

data class VerificationGateInitResult(
    /** Requirements the identity can still act on. */
    val requirements: List<VerificationRequirement>,
    /** Requirements already submitted and awaiting review. */
    val pendingRequirements: List<PendingVerificationRequirement>,
    /** Single-use submit nonce — pass to `submit()`. */
    val csrfToken: String,
    val returnUrl: String
)

data class SubmitDocumentConfirmation @JvmOverloads constructor(
    val requirementKey: String,
    /** The [VerificationUploadIntent.key] used for the completed PUT. */
    val s3Key: String,
    val documentType: String? = null,
    val side: String? = null,
    val originalFilename: String? = null
)

data class SubmitFormAnswer(
    val requirementKey: String,
    /** Answer values keyed by the requirement's [RequirementField.key]. */
    val values: Map<String, String>
)

data class VerificationGateSubmitParams @JvmOverloads constructor(
    val token: String,
    /** The single-use nonce from `init()`. */
    val csrfToken: String,
    val documents: List<SubmitDocumentConfirmation>? = null,
    val answers: List<SubmitFormAnswer>? = null
)

data class VerificationGateSubmitResult(
    val returnUrl: String,
    /**
     * Recording confirms the submission reached compliance — it does not
     * mean the requirement is now satisfied. A customer's own submission is
     * always recorded as `pending_review` and requires ops review before it
     * counts toward the identity's effective tier.
     */
    val documentsRecorded: Int,
    /** One evidence record per submitted requirement key. */
    val answersRecorded: Int
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

@Serializable
internal data class RequirementFieldWire(
    val key: String,
    val label: String,
    val type: String,
    val required: Boolean? = null,
    val options: List<String>? = null
)

@Serializable
internal data class TierRequirementStatusWire(
    val key: String,
    val kind: String,
    val status: String,
    val description: String? = null,
    val fields: List<RequirementFieldWire>? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
internal data class TierLevelStatusWire(
    val level: Int,
    val name: String,
    val satisfied: Boolean,
    val requirements: List<TierRequirementStatusWire>
)

@Serializable
internal data class VerificationFlowHandoffWire(
    val type: String,
    val method: String,
    @SerialName("start_endpoint") val startEndpoint: String,
    @SerialName("response_url_field") val responseUrlField: String
)

@Serializable
internal data class GetTierStatusResponseWire(
    @SerialName("identity_urn") val identityUrn: String,
    @SerialName("effective_level") val effectiveLevel: Int,
    @SerialName("policy_version") val policyVersion: String,
    val levels: List<TierLevelStatusWire>,
    @SerialName("next_level") val nextLevel: Int? = null,
    @SerialName("missing_requirements") val missingRequirements: List<String>? = null,
    @SerialName("pending_requirements") val pendingRequirements: List<String>? = null,
    @SerialName("verification_flow") val verificationFlow: VerificationFlowHandoffWire? = null
)

@Serializable
internal data class GateStartRequestWire(@SerialName("return_url") val returnUrl: String? = null)

@Serializable
internal data class GateStartResponseWire(
    val token: String,
    val url: String,
    @SerialName("expires_in") val expiresIn: String
)

@Serializable
internal data class TosGateDocumentWire(
    @SerialName("document_version_id") val documentVersionId: String,
    @SerialName("version_label") val versionLabel: String,
    @SerialName("content_hash") val contentHash: String,
    val content: String
)

@Serializable
internal data class TosGateInitResponseWire(
    val document: TosGateDocumentWire,
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("return_url") val returnUrl: String,
    @SerialName("show_home") val showHome: Boolean = true
)

@Serializable
internal data class TosGateAcceptRequestWire(
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("device_attestation") val deviceAttestation: String? = null
)

@Serializable
internal data class TosAcceptanceRecordWire(
    val id: String,
    @SerialName("identity_urn") val identityUrn: String,
    @SerialName("document_version_id") val documentVersionId: String,
    @SerialName("document_version_label") val documentVersionLabel: String,
    @SerialName("document_hash") val documentHash: String,
    @SerialName("accepted_at") val acceptedAt: String,
    @SerialName("auth_assurance") val authAssurance: String
)

@Serializable
internal data class TosGateAcceptResponseWire(
    val acceptance: TosAcceptanceRecordWire,
    @SerialName("return_url") val returnUrl: String
)

@Serializable
internal data class VerificationUploadIntentWire(
    @SerialName("content_type") val contentType: String,
    val key: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("max_size_bytes") val maxSizeBytes: Long
)

@Serializable
internal data class VerificationRequirementWire(
    val key: String,
    val kind: String,
    val description: String? = null,
    val fields: List<RequirementFieldWire>? = null,
    val uploadable: Boolean,
    @SerialName("upload_intents") val uploadIntents: List<VerificationUploadIntentWire>? = null
)

@Serializable
internal data class PendingVerificationRequirementWire(
    val key: String,
    val description: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
internal data class VerificationGateInitResponseWire(
    val requirements: List<VerificationRequirementWire>,
    @SerialName("pending_requirements") val pendingRequirements: List<PendingVerificationRequirementWire>? = null,
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("return_url") val returnUrl: String? = null
)

@Serializable
internal data class VerificationGateSubmitDocumentWire(
    @SerialName("requirement_key") val requirementKey: String,
    @SerialName("s3_key") val s3Key: String,
    @SerialName("document_type") val documentType: String? = null,
    val side: String? = null,
    @SerialName("original_filename") val originalFilename: String? = null
)

@Serializable
internal data class VerificationGateSubmitAnswerWire(
    @SerialName("requirement_key") val requirementKey: String,
    val values: Map<String, String>
)

@Serializable
internal data class VerificationGateSubmitRequestWire(
    @SerialName("csrf_token") val csrfToken: String,
    val documents: List<VerificationGateSubmitDocumentWire>? = null,
    val answers: List<VerificationGateSubmitAnswerWire>? = null
)

@Serializable
internal data class VerificationGateSubmitResponseWire(
    @SerialName("return_url") val returnUrl: String? = null,
    val documents: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    val answers: List<kotlinx.serialization.json.JsonElement> = emptyList()
)
