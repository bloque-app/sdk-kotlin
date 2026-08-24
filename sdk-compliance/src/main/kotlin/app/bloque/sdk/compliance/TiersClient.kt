package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private fun mapRequirementFieldType(type: String): RequirementFieldType = when (type) {
    "text" -> RequirementFieldType.TEXT
    "number" -> RequirementFieldType.NUMBER
    "date" -> RequirementFieldType.DATE
    "select" -> RequirementFieldType.SELECT
    "boolean" -> RequirementFieldType.BOOLEAN
    else -> RequirementFieldType.TEXT
}

/**
 * Normalizes a single wire `select` option — a bare JSON string (legacy,
 * unlocalized) or `{ value, label: { en, es } }` — into [RequirementFieldOption].
 * Anything unparseable degrades to a plain, unlocalized option using the
 * element's own string form, rather than dropping the option entirely.
 */
private fun mapRequirementFieldOption(element: JsonElement): RequirementFieldOption {
    return when {
        element is JsonPrimitive && element.isString -> RequirementFieldOption(value = element.content)
        element is JsonObject -> {
            val value = (element["value"] as? JsonPrimitive)?.content ?: ""
            val labelObj = element["label"] as? JsonObject
            val label = labelObj?.let {
                LocalizedText(
                    en = (it["en"] as? JsonPrimitive)?.content ?: "",
                    es = (it["es"] as? JsonPrimitive)?.content ?: ""
                )
            }
            RequirementFieldOption(value = value, label = label)
        }
        else -> RequirementFieldOption(value = element.toString())
    }
}

internal fun mapRequirementField(wire: RequirementFieldWire): RequirementField {
    return RequirementField(
        key = wire.key,
        label = wire.label,
        type = mapRequirementFieldType(wire.type),
        required = wire.required,
        description = wire.description,
        options = wire.options?.map(::mapRequirementFieldOption),
        locale = wire.locale
    )
}

private fun mapRequirementEvidenceStatus(status: String): RequirementEvidenceStatus = when (status) {
    "satisfied" -> RequirementEvidenceStatus.SATISFIED
    "not_satisfied" -> RequirementEvidenceStatus.NOT_SATISFIED
    "expired" -> RequirementEvidenceStatus.EXPIRED
    "revoked" -> RequirementEvidenceStatus.REVOKED
    "pending_review" -> RequirementEvidenceStatus.PENDING_REVIEW
    else -> RequirementEvidenceStatus.NOT_SATISFIED
}

private fun mapRequirementStatus(wire: TierRequirementStatusWire): TierRequirementStatus {
    return TierRequirementStatus(
        key = wire.key,
        kind = wire.kind,
        status = mapRequirementEvidenceStatus(wire.status),
        description = wire.description,
        title = wire.title,
        fields = wire.fields?.map(::mapRequirementField),
        submittedAt = wire.submittedAt,
        requiresUpload = wire.requiresUpload,
        graceUntil = wire.graceUntil
    )
}

private fun mapRequirementUploadIntent(wire: RequirementUploadIntentResponseWire): RequirementUploadIntent {
    return RequirementUploadIntent(
        key = wire.key,
        uploadUrl = wire.uploadUrl,
        contentType = wire.contentType,
        acl = wire.acl,
        serverSideEncryption = wire.serverSideEncryption,
        expiresInSeconds = wire.expiresInSeconds,
        maxSizeBytes = wire.maxSizeBytes
    )
}

private fun mapRequirementDocument(wire: RequirementDocumentResponseWire): RequirementDocument {
    return RequirementDocument(
        id = wire.id,
        complianceId = wire.complianceId,
        identityUrn = wire.identityUrn,
        requirementKey = wire.requirementKey,
        documentType = wire.documentType,
        side = wire.side,
        imageS3Key = wire.imageS3Key,
        imageSizeBytes = wire.imageSizeBytes,
        contentType = wire.contentType,
        originalFilename = wire.originalFilename,
        sha256 = wire.sha256,
        uploadedBy = wire.uploadedBy,
        createdAt = wire.createdAt,
        downloadUrl = wire.downloadUrl
    )
}

private fun mapVerificationFlowType(type: String): VerificationFlowType = when (type) {
    "tos_hosted_acceptance" -> VerificationFlowType.TOS_HOSTED_ACCEPTANCE
    "document_submission" -> VerificationFlowType.DOCUMENT_SUBMISSION
    else -> VerificationFlowType.DOCUMENT_SUBMISSION
}

internal fun mapVerificationFlow(wire: VerificationFlowHandoffWire?): VerificationFlowHandoff? {
    if (wire == null) return null
    return VerificationFlowHandoff(
        type = mapVerificationFlowType(wire.type),
        method = wire.method,
        startEndpoint = wire.startEndpoint,
        responseUrlField = wire.responseUrlField
    )
}

/**
 * Tier status client — the compliance engine's verification-tier control
 * plane. [getStatus] is the read side; [createRequirementUploadIntent],
 * [confirmRequirementUpload], and [listRequirementDocuments] are a
 * self-servable evidence-upload path for `document`/`manual_review`
 * requirements, authorized by the identity's own JWT and independent of the
 * hosted verification gate (which mints its own upload intents via
 * `verificationGate.init()` instead).
 */
class TiersClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Get an identity's effective compliance tier, per-level requirement
     * status, and (if not fully verified) which requirements are missing and
     * which hosted gate resolves them.
     *
     * @param params URN of the identity whose tier status is being read
     * @return the identity's tier status
     */
    fun getStatus(params: GetTierStatusParams): TierStatus {
        val response = httpClient.get<GetTierStatusResponseWire>(
            path = "/api/compliance/${params.urn}/tier-status"
        )

        return TierStatus(
            identityUrn = response.identityUrn,
            effectiveLevel = response.effectiveLevel,
            policyVersion = response.policyVersion,
            levels = response.levels.map { level ->
                TierLevelStatus(
                    level = level.level,
                    name = level.name,
                    satisfied = level.satisfied,
                    requirements = level.requirements.map(::mapRequirementStatus)
                )
            },
            nextLevel = response.nextLevel,
            missingRequirements = response.missingRequirements ?: emptyList(),
            pendingRequirements = response.pendingRequirements ?: emptyList(),
            verificationFlow = mapVerificationFlow(response.verificationFlow),
            nextRecomputeAt = response.nextRecomputeAt
        )
    }

    /**
     * Mint a short-lived presigned upload URL for a `document`/`manual_review`
     * requirement's evidence. Self-servable by the identity's own JWT —
     * independent of the hosted verification gate. The S3 key is always
     * server-derived; upload the file with an HTTP PUT to
     * [RequirementUploadIntent.uploadUrl], then confirm with
     * [confirmRequirementUpload].
     *
     * @param params URN, requirement key, and the file's content type
     * @return a presigned PUT URL and its server-derived key
     */
    fun createRequirementUploadIntent(params: CreateRequirementUploadIntentParams): RequirementUploadIntent {
        val response = httpClient.post<RequirementUploadIntentResponseWire, CreateRequirementUploadIntentRequestWire>(
            path = "/api/compliance/${params.urn}/requirements/${params.requirementKey}/upload-intent",
            body = CreateRequirementUploadIntentRequestWire(
                contentType = params.contentType,
                sizeBytes = params.sizeBytes
            )
        )
        return mapRequirementUploadIntent(response)
    }

    /**
     * Confirm a completed PUT upload from [createRequirementUploadIntent] and
     * record it as evidence pending review. Does not by itself satisfy the
     * requirement or recompute the tier — a reviewer decision still has to
     * land.
     *
     * @param params URN, requirement key, the confirmed S3 key, and document metadata
     * @return the recorded document
     */
    fun confirmRequirementUpload(params: ConfirmRequirementUploadParams): RequirementDocument {
        val response = httpClient.post<RequirementDocumentResponseWire, ConfirmRequirementUploadRequestWire>(
            path = "/api/compliance/${params.urn}/requirements/${params.requirementKey}/documents",
            body = ConfirmRequirementUploadRequestWire(
                s3Key = params.s3Key,
                documentType = params.documentType,
                side = params.side,
                originalFilename = params.originalFilename
            )
        )
        return mapRequirementDocument(response)
    }

    /**
     * List every evidence document uploaded for a requirement, each with a
     * short-lived presigned `downloadUrl` where available.
     *
     * @param params URN and requirement key
     * @return the requirement's uploaded documents
     */
    fun listRequirementDocuments(params: ListRequirementDocumentsParams): List<RequirementDocument> {
        val response = httpClient.get<List<RequirementDocumentResponseWire>>(
            path = "/api/compliance/${params.urn}/requirements/${params.requirementKey}/documents"
        )
        return response.map(::mapRequirementDocument)
    }
}
