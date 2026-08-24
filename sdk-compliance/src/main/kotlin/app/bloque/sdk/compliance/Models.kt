package app.bloque.sdk.compliance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
 *
 * [accompliceType] and [webhookUrl] are accepted here for forward
 * compatibility but are **not currently sent to the API** — the
 * `POST /compliance` controller's request body only reads `urn`/`type`
 * today, so both fields would be silently dropped server-side. Kept as
 * public params rather than removed outright in case a future server
 * revision starts honoring them.
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

/** A single automated verification check the compliance provider ran. */
data class ComplianceVerificationCheck @JvmOverloads constructor(
    val verified: Boolean,
    val comment: String,
    val declineReasons: List<String> = emptyList()
)

/** The database/watchlist screening check, which additionally reports which databases were checked. */
data class ComplianceDatabaseScreeningCheck @JvmOverloads constructor(
    val verified: Boolean,
    val comment: String,
    val declineReasons: List<String> = emptyList(),
    val databases: List<String> = emptyList()
)

/** All automated checks the compliance provider ran for a verification. */
data class ComplianceVerificationChecks @JvmOverloads constructor(
    val profile: ComplianceVerificationCheck,
    val document: ComplianceVerificationCheck,
    val facial: ComplianceVerificationCheck? = null,
    val databaseScreening: ComplianceDatabaseScreeningCheck,
    val adverseMedia: ComplianceVerificationCheck? = null
)

/** Applicant details as extracted/confirmed by the compliance provider. */
data class ComplianceApplicant @JvmOverloads constructor(
    val firstName: String,
    val lastName: String,
    val dob: String,
    val gender: String,
    val nationality: String,
    val residenceCountry: String,
    val email: String? = null,
    val phone: String? = null
)

/** A single identity document the provider extracted during verification. */
data class ComplianceVerificationDocument @JvmOverloads constructor(
    val type: String,
    val mappedType: String,
    val documentNumber: String,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val issuingAuthority: String? = null,
    val status: String
)

/** Client network/geo context captured at verification time, if available. */
data class ComplianceClientInfo @JvmOverloads constructor(
    val ip: String,
    val countryCode: String,
    val city: String? = null
)

/**
 * The provider-agnostic, standardized verification result — present once the
 * provider has actually returned a verdict (not while a verification is
 * still awaiting completion).
 */
data class ComplianceVerificationResult @JvmOverloads constructor(
    val verified: Boolean,
    val providerReference: String,
    val checks: ComplianceVerificationChecks,
    val applicant: ComplianceApplicant,
    val documents: List<ComplianceVerificationDocument> = emptyList(),
    val clientInfo: ComplianceClientInfo? = null,
    val faceMatchConfidence: Double? = null,
    val declineReasons: List<String> = emptyList()
)

/**
 * KYC verification response
 */
data class KycVerificationResponse @JvmOverloads constructor(
    val status: KycVerificationStatus,
    val url: String,
    val completedAt: String? = null,
    /** The standardized verification result, once the provider has returned a verdict. */
    val result: ComplianceVerificationResult? = null,
    /** Status of the underlying document image downloads (e.g. `"pending"`, `"complete"`, `"partial"`, `"failed"`, `"skipped"`). */
    val documentsStatus: String? = null
)

/** A single document image on file for a KYC verification. */
data class KycDocument @JvmOverloads constructor(
    val documentType: String,
    val side: String,
    val imageS3Key: String? = null,
    val imageSizeBytes: Long? = null,
    /** Short-lived presigned URL to view the image, when available. */
    val downloadUrl: String? = null
)

/** Response of [KycClient.getDocuments]. */
data class KycDocumentsResult @JvmOverloads constructor(
    val documentsStatus: String? = null,
    val documents: List<KycDocument> = emptyList()
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

/** `{ en, es }` pair used wherever a requirement/field surfaces a
 * display string in more than one language. */
data class LocalizedText(
    val en: String,
    val es: String
)

/**
 * A single [RequirementFieldType.SELECT] choice with a stored [value]
 * distinct from its localized display [label].
 *
 * The wire form can be either a legacy plain string (rendered as-is,
 * unlocalized — [label] comes back `null`) or the newer
 * `{ value, label: { en, es } }` shape. Both are normalized into this one
 * type so callers never need to branch on wire shape themselves.
 */
data class RequirementFieldOption @JvmOverloads constructor(
    val value: String,
    val label: LocalizedText? = null
)

/**
 * A single field definition for a requirement that collects form answers.
 */
data class RequirementField @JvmOverloads constructor(
    val key: String,
    val label: String,
    val type: RequirementFieldType,
    val required: Boolean? = null,
    /** Short help text rendered under the label, clarifying what's being asked. */
    val description: String? = null,
    /** Only meaningful for [RequirementFieldType.SELECT]. */
    val options: List<RequirementFieldOption>? = null,
    /**
     * Pins which side of a localized [RequirementFieldOption.label] to
     * display for this field, overriding whatever locale detection the
     * caller would otherwise apply. `null` means "use the caller's own
     * locale detection" (e.g. `Accept-Language` on a hosted page).
     */
    val locale: String? = null
)

/** Status of a single requirement within a tier level. */
data class TierRequirementStatus @JvmOverloads constructor(
    val key: String,
    /** e.g. `"tos"`, `"kyc"`, `"document"`, `"manual_review"`, `"provider_check"`. */
    val kind: String,
    val status: RequirementEvidenceStatus,
    /** What this requirement means, for display without hardcoding key strings. */
    val description: String? = null,
    /** Human-readable title for the requirement's card. Falls back to a
     * humanized [key] client-side when absent (older policies). */
    val title: String? = null,
    /** Only present for requirements that collect form answers. */
    val fields: List<RequirementField>? = null,
    /** ISO-8601 timestamp of the submission behind a `PENDING_REVIEW` status. */
    val submittedAt: String? = null,
    /**
     * When explicitly `false`, this requirement is form-only and must
     * never be treated as uploadable regardless of [kind] (e.g. a
     * `manual_review` declaration with no accompanying document).
     * `null`/`true` preserves the usual kind-based uploadable default.
     */
    val requiresUpload: Boolean? = null,
    /**
     * ISO-8601. Set only while this requirement reads [RequirementEvidenceStatus.SATISFIED]
     * purely because of a rollout `enforcement_starts_at` window (a TOS document
     * cutoff, or a generic requirement's own cutoff) — never for the
     * pre-existing `grace_period_days` window, and never from any other
     * requirement kind's expiry. Lets a client still prompt "you have until
     * X to accept/submit" even though nothing is currently blocking the
     * identity, which matters exactly because nothing is blocking them:
     * [TierStatus.missingRequirements]/[TierStatus.verificationFlow] go
     * quiet for the whole window otherwise.
     */
    val graceUntil: String? = null
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

// ============================================
// Self-Servable Requirement Documents
// (independent of the hosted verification gate — the identity's own JWT,
// or a service credential acting on its behalf, can call these directly)
// ============================================

/** Parameters for [TiersClient.createRequirementUploadIntent]. */
data class CreateRequirementUploadIntentParams @JvmOverloads constructor(
    val urn: String,
    val requirementKey: String,
    /** Allowed: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`. */
    val contentType: String,
    /** Declared size, in bytes. Validated fail-fast; the server also
     * re-validates the actual object size once uploaded. */
    val sizeBytes: Long? = null
)

/** A presigned upload URL for a single requirement's evidence document. */
data class RequirementUploadIntent @JvmOverloads constructor(
    /** The server-derived S3 key — pass to [ConfirmRequirementUploadParams.s3Key]. */
    val key: String,
    /** Presigned URL for a direct PUT upload. */
    val uploadUrl: String,
    val contentType: String,
    val acl: String,
    val serverSideEncryption: String,
    val expiresInSeconds: Int,
    val maxSizeBytes: Long
)

/** Parameters for [TiersClient.confirmRequirementUpload]. */
data class ConfirmRequirementUploadParams @JvmOverloads constructor(
    val urn: String,
    val requirementKey: String,
    /** The [RequirementUploadIntent.key] used for the completed PUT. */
    val s3Key: String,
    val documentType: String,
    val side: String? = null,
    val originalFilename: String? = null
)

/** Parameters for [TiersClient.listRequirementDocuments]. */
data class ListRequirementDocumentsParams(
    val urn: String,
    val requirementKey: String
)

/**
 * A single evidence document recorded for a tier requirement. Recording it
 * does not by itself satisfy the requirement — a reviewer decision still
 * has to land (see [RequirementEvidenceStatus.PENDING_REVIEW]).
 */
data class RequirementDocument @JvmOverloads constructor(
    val id: String,
    val complianceId: String? = null,
    val identityUrn: String? = null,
    val requirementKey: String? = null,
    val documentType: String,
    val side: String,
    val imageS3Key: String? = null,
    val imageSizeBytes: Long? = null,
    val contentType: String? = null,
    val originalFilename: String? = null,
    val sha256: String? = null,
    val uploadedBy: String? = null,
    val createdAt: String? = null,
    /** Short-lived presigned download URL — only populated by [TiersClient.listRequirementDocuments]. */
    val downloadUrl: String? = null
)

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
    val verificationFlow: VerificationFlowHandoff? = null,
    /**
     * Earliest instant this status could change with no further input from
     * the customer — a TOS grace-period deadline or an evidence expiry.
     * `null` when nothing time-driven is pending, i.e. the next change (if
     * any) requires new evidence rather than the mere passage of time.
     * Useful as a polling hint: there is no need to re-poll before this
     * timestamp.
     */
    val nextRecomputeAt: String? = null
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

/** Parameters shared by [TosGateClient.init] and [TosGateClient.challenge]. */
data class TosGateInitParams(val token: String)

data class TosGateDocument(
    val documentVersionId: String,
    val versionLabel: String,
    val contentHash: String,
    /** Rendered document content (Markdown-templated to HTML server-side). */
    val content: String
)

data class TosGateInitResult @JvmOverloads constructor(
    val document: TosGateDocument,
    /** Single-use acceptance nonce — pass to `accept()`. */
    val csrfToken: String,
    val returnUrl: String,
    /** Whether the hosted page's intro screens should play before the document. */
    val showHome: Boolean,
    /**
     * The calling origin's `gate_accent_color` (strict 3-/6-digit CSS hex),
     * if it configured one. `null` uses the hosted page's default brand
     * color. Only meaningful if you're rendering your own UI around the
     * hosted page (e.g. a WebView chrome) — the hosted page itself already
     * applies this automatically.
     */
    val accentColor: String? = null,
    /** The calling origin's display name, for replacing the hosted page's
     * default "bloque" wordmark. `null`/blank keeps the page default. */
    val developerName: String? = null,
    /**
     * Whether this identity should be offered a WebAuthn passkey step
     * (see [TosGateClient.challenge]). `false` means the gate is a plain
     * click-to-accept.
     */
    val passkeyRequired: Boolean = false
)

/**
 * The challenge a browser's authenticator must answer to register a
 * passkey as this identity's PassAccount device. Bound to a specific chain
 * block ([context]) with a limited answer window ([expiresAtBlock]) — mint
 * it as late as possible (see [TosGateClient.challenge]).
 */
data class TosGatePasskeyChallenge @JvmOverloads constructor(
    val challenge: String,
    val context: Long,
    /** Last block the challenge can still be answered at. */
    val expiresAtBlock: Long,
    val userId: String,
    val userName: String,
    /** 0x-hex public key address for the account being activated. */
    val publicAddress: String
)

/**
 * Result of [TosGateClient.challenge]. [passkey] is `null` when this
 * identity has no account ready for a device — the gate should then show
 * no passkey step.
 */
data class TosGateChallengeResult(val passkey: TosGatePasskeyChallenge?)

/**
 * The raw WebAuthn registration parts produced by a browser's authenticator,
 * as an alternative to a pre-built [TosGateAttestation.DeviceAttestation].
 * The origins controller frames these into the same attestation shape
 * itself. All byte fields are base64url (what a browser puts in a JSON body).
 */
data class TosGatePasskeyRegistration @JvmOverloads constructor(
    /** `PublicKeyCredential.rawId`. */
    val credentialId: String,
    /** `getAuthenticatorData()`. */
    val authenticatorData: String,
    /** `clientDataJSON`. */
    val clientData: String,
    /** `getPublicKey()`, SPKI. */
    val publicKey: String,
    /** The [TosGatePasskeyChallenge.context] this challenge was minted with. */
    val context: Long
)

/**
 * Either a finished Pass device attestation, or the raw passkey registration
 * parts for the server to frame into one — mirrors the API's `oneOf` for
 * `device_attestation`/`passkey` on `POST /api/tos-gate/accept`. `null` on
 * [TosGateAcceptParams.attestation] means a plain click-to-accept with no
 * device handoff.
 */
sealed class TosGateAttestation {
    /** A pre-built Pass device attestation (0x-hex SCALE-encoded `PassDeviceAttestation`). */
    data class DeviceAttestation(val hex: String) : TosGateAttestation()

    /** The raw WebAuthn registration the hosted gate/SDK collected. */
    data class Passkey(val registration: TosGatePasskeyRegistration) : TosGateAttestation()
}

data class TosGateAcceptParams @JvmOverloads constructor(
    /** The capability token returned by `start()`. */
    val token: String,
    /** The single-use nonce returned by `init()`. */
    val csrfToken: String,
    /**
     * When present, accepting the terms also hands control of the
     * identity's Kreivo PassAccount to the device behind this attestation —
     * either a pre-built [TosGateAttestation.DeviceAttestation] or the raw
     * [TosGateAttestation.Passkey] parts for the server to frame itself.
     * `null` is a plain click-to-accept with no device handoff.
     */
    val attestation: TosGateAttestation? = null
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
    /** Human-readable title for the requirement's card. Falls back to a
     * humanized [key] client-side when absent (older policies). */
    val title: String? = null,
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
    /** Human-readable title for the requirement's card. Falls back to a
     * humanized [key] client-side when absent (older policies). */
    val title: String? = null,
    /** ISO-8601 timestamp of the submission. */
    val submittedAt: String? = null
)

data class VerificationGateInitResult @JvmOverloads constructor(
    /** Requirements the identity can still act on. */
    val requirements: List<VerificationRequirement>,
    /** Requirements already submitted and awaiting review. */
    val pendingRequirements: List<PendingVerificationRequirement>,
    /** Single-use submit nonce — pass to `submit()`. */
    val csrfToken: String,
    val returnUrl: String,
    /**
     * The calling origin's `gate_accent_color` (strict 3-/6-digit CSS hex),
     * if it configured one. `null` uses the hosted page's default brand
     * color. Only meaningful if you're rendering your own UI around the
     * hosted page — the hosted page itself already applies this
     * automatically.
     */
    val accentColor: String? = null,
    /** The calling origin's display name, for replacing the hosted page's
     * default "bloque" wordmark. `null`/blank keeps the page default. */
    val developerName: String? = null
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

/**
 * `accomplice_type`/`webhook_url` are deliberately absent here: the
 * `POST /compliance` controller's actual body type only reads `urn`/`type`,
 * so sending them would do nothing. See [KycVerificationParams]'s doc
 * comment.
 */
@Serializable
internal data class KycVerificationRequestWire(
    val urn: String,
    val type: String = "kyc"
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
    val provider: String? = null,
    val result: ComplianceVerificationResultWire? = null,
    @SerialName("documents_status") val documentsStatus: String? = null
)

@Serializable
internal data class ComplianceVerificationCheckWire(
    val verified: Boolean,
    val comment: String,
    @SerialName("decline_reasons") val declineReasons: List<String> = emptyList()
)

@Serializable
internal data class ComplianceDatabaseScreeningCheckWire(
    val verified: Boolean,
    val comment: String,
    @SerialName("decline_reasons") val declineReasons: List<String> = emptyList(),
    val databases: List<String> = emptyList()
)

@Serializable
internal data class ComplianceVerificationChecksWire(
    val profile: ComplianceVerificationCheckWire,
    val document: ComplianceVerificationCheckWire,
    val facial: ComplianceVerificationCheckWire? = null,
    @SerialName("database_screening") val databaseScreening: ComplianceDatabaseScreeningCheckWire,
    @SerialName("adverse_media") val adverseMedia: ComplianceVerificationCheckWire? = null
)

@Serializable
internal data class ComplianceApplicantWire(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val dob: String,
    val gender: String,
    val nationality: String,
    @SerialName("residence_country") val residenceCountry: String,
    val email: String? = null,
    val phone: String? = null
)

@Serializable
internal data class ComplianceVerificationDocumentWire(
    val type: String,
    @SerialName("mapped_type") val mappedType: String,
    @SerialName("document_number") val documentNumber: String,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("issuing_authority") val issuingAuthority: String? = null,
    val status: String
)

@Serializable
internal data class ComplianceClientInfoWire(
    val ip: String,
    @SerialName("country_code") val countryCode: String,
    val city: String? = null
)

@Serializable
internal data class ComplianceVerificationResultWire(
    val verified: Boolean,
    @SerialName("provider_reference") val providerReference: String,
    val checks: ComplianceVerificationChecksWire,
    val applicant: ComplianceApplicantWire,
    val documents: List<ComplianceVerificationDocumentWire> = emptyList(),
    @SerialName("client_info") val clientInfo: ComplianceClientInfoWire? = null,
    @SerialName("face_match_confidence") val faceMatchConfidence: Double? = null,
    @SerialName("decline_reasons") val declineReasons: List<String> = emptyList()
)

@Serializable
internal data class KycDocumentWire(
    @SerialName("document_type") val documentType: String,
    val side: String,
    @SerialName("image_s3_key") val imageS3Key: String? = null,
    @SerialName("image_size_bytes") val imageSizeBytes: Long? = null,
    @SerialName("download_url") val downloadUrl: String? = null
)

@Serializable
internal data class KycDocumentsResponseWire(
    @SerialName("documents_status") val documentsStatus: String? = null,
    val documents: List<KycDocumentWire> = emptyList()
)

@Serializable
internal data class LocalizedTextWire(
    val en: String = "",
    val es: String = ""
)

/**
 * Wire shape of a single `select` option: either a bare JSON string
 * (legacy, unlocalized) or `{ value, label: { en, es } }`. Decoded as raw
 * [JsonElement]s and normalized by [mapRequirementFieldOption] rather than
 * a polymorphic `@Serializable` union, matching this file's existing
 * convention for wire fields that mix shapes (see
 * `VerificationGateSubmitResponseWire`'s `documents`/`answers`).
 */
@Serializable
internal data class RequirementFieldWire(
    val key: String,
    val label: String,
    val type: String,
    val required: Boolean? = null,
    val description: String? = null,
    val options: List<JsonElement>? = null,
    val locale: String? = null
)

@Serializable
internal data class TierRequirementStatusWire(
    val key: String,
    val kind: String,
    val status: String,
    val description: String? = null,
    val title: String? = null,
    val fields: List<RequirementFieldWire>? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("requires_upload") val requiresUpload: Boolean? = null,
    @SerialName("grace_until") val graceUntil: String? = null
)

@Serializable
internal data class CreateRequirementUploadIntentRequestWire(
    @SerialName("content_type") val contentType: String,
    @SerialName("size_bytes") val sizeBytes: Long? = null
)

@Serializable
internal data class RequirementUploadIntentResponseWire(
    val key: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("content_type") val contentType: String,
    val acl: String,
    @SerialName("server_side_encryption") val serverSideEncryption: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int,
    @SerialName("max_size_bytes") val maxSizeBytes: Long
)

@Serializable
internal data class ConfirmRequirementUploadRequestWire(
    @SerialName("s3_key") val s3Key: String,
    @SerialName("document_type") val documentType: String,
    val side: String? = null,
    @SerialName("original_filename") val originalFilename: String? = null
)

@Serializable
internal data class RequirementDocumentResponseWire(
    val id: String,
    @SerialName("compliance_id") val complianceId: String? = null,
    @SerialName("identity_urn") val identityUrn: String? = null,
    @SerialName("requirement_key") val requirementKey: String? = null,
    @SerialName("document_type") val documentType: String,
    val side: String,
    @SerialName("image_s3_key") val imageS3Key: String? = null,
    @SerialName("image_size_bytes") val imageSizeBytes: Long? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("original_filename") val originalFilename: String? = null,
    val sha256: String? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null
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
    @SerialName("verification_flow") val verificationFlow: VerificationFlowHandoffWire? = null,
    @SerialName("next_recompute_at") val nextRecomputeAt: String? = null
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
    @SerialName("show_home") val showHome: Boolean = true,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("developer_name") val developerName: String? = null,
    @SerialName("passkey_required") val passkeyRequired: Boolean = false
)

@Serializable
internal data class TosGatePasskeyChallengeWire(
    val challenge: String,
    val context: Long,
    @SerialName("expires_at_block") val expiresAtBlock: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("public_address") val publicAddress: String
)

@Serializable
internal data class TosGateChallengeResponseWire(
    val passkey: TosGatePasskeyChallengeWire? = null
)

@Serializable
internal data class TosGatePasskeyRegistrationWire(
    @SerialName("credential_id") val credentialId: String,
    @SerialName("authenticator_data") val authenticatorData: String,
    @SerialName("client_data") val clientData: String,
    @SerialName("public_key") val publicKey: String,
    val context: Long
)

@Serializable
internal data class TosGateAcceptRequestWire(
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("device_attestation") val deviceAttestation: String? = null,
    val passkey: TosGatePasskeyRegistrationWire? = null
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
    val title: String? = null,
    val fields: List<RequirementFieldWire>? = null,
    val uploadable: Boolean,
    @SerialName("upload_intents") val uploadIntents: List<VerificationUploadIntentWire>? = null
)

@Serializable
internal data class PendingVerificationRequirementWire(
    val key: String,
    val description: String? = null,
    val title: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
internal data class VerificationGateInitResponseWire(
    val requirements: List<VerificationRequirementWire>,
    @SerialName("pending_requirements") val pendingRequirements: List<PendingVerificationRequirementWire>? = null,
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("return_url") val returnUrl: String? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("developer_name") val developerName: String? = null
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
