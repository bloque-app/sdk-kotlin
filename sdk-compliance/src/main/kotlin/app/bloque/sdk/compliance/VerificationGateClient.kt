package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

private fun mapUploadIntent(wire: VerificationUploadIntentWire): VerificationUploadIntent {
    return VerificationUploadIntent(
        contentType = wire.contentType,
        key = wire.key,
        uploadUrl = wire.uploadUrl,
        maxSizeBytes = wire.maxSizeBytes
    )
}

private fun mapRequirement(wire: VerificationRequirementWire): VerificationRequirement {
    return VerificationRequirement(
        key = wire.key,
        kind = wire.kind,
        description = wire.description,
        title = wire.title,
        fields = wire.fields?.map(::mapRequirementField),
        uploadable = wire.uploadable,
        uploadIntents = wire.uploadIntents?.map(::mapUploadIntent)
    )
}

private fun mapPendingRequirement(wire: PendingVerificationRequirementWire): PendingVerificationRequirement {
    return PendingVerificationRequirement(
        key = wire.key,
        description = wire.description,
        title = wire.title,
        submittedAt = wire.submittedAt
    )
}

private fun mapDocumentToWire(document: SubmitDocumentConfirmation): VerificationGateSubmitDocumentWire {
    return VerificationGateSubmitDocumentWire(
        requirementKey = document.requirementKey,
        s3Key = document.s3Key,
        documentType = document.documentType,
        side = document.side,
        originalFilename = document.originalFilename
    )
}

private fun mapAnswerToWire(answer: SubmitFormAnswer): VerificationGateSubmitAnswerWire {
    return VerificationGateSubmitAnswerWire(
        requirementKey = answer.requirementKey,
        values = answer.values
    )
}

/**
 * Phase 3 hosted verification gate (`/api/verification-gate` routes) — the
 * single hosted page that collects both document uploads and form answers
 * for any outstanding Level 2+ (non-TOS/non-KYC) requirement.
 *
 * Same capability-token shape as [TosGateClient]: [start] uses the SDK's
 * session auth, [init]/[submit] authenticate solely via the capability
 * `token`.
 *
 * Usually you won't call these directly: catch a
 * `BloqueVerificationRequiredError` with `reason == "documents"` and call
 * its `getVerificationLink()` instead, which calls [start] for you.
 *
 * A `BloqueVerificationPendingError` is the one case where you should not
 * open this gate at all — everything it would collect is already with a
 * reviewer.
 */
class VerificationGateClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Mint a portable verification gate capability token + hosted page URL.
     */
    fun start(params: StartVerificationGateParams): StartGateResult {
        val response = httpClient.post<GateStartResponseWire, GateStartRequestWire>(
            path = "/api/verification-gate/start",
            body = GateStartRequestWire(returnUrl = params.returnUrl)
        )
        return StartGateResult(token = response.token, url = response.url, expiresIn = response.expiresIn)
    }

    /**
     * Fetch the token identity's actionable requirements — with
     * descriptions, form field definitions, and presigned upload URLs for
     * uploadable ones — plus anything already under review (reported
     * separately, never re-collected) and a single-use submit nonce.
     * Authorized solely by [params]'s token.
     */
    fun init(params: VerificationGateInitParams): VerificationGateInitResult {
        val response = httpClient.get<VerificationGateInitResponseWire>(
            path = "/api/verification-gate/init",
            headers = mapOf("Authorization" to "Bearer ${params.token}")
        )
        return VerificationGateInitResult(
            requirements = response.requirements.map(::mapRequirement),
            pendingRequirements = (response.pendingRequirements ?: emptyList()).map(::mapPendingRequirement),
            csrfToken = response.csrfToken,
            returnUrl = response.returnUrl ?: "",
            accentColor = response.accentColor
        )
    }

    /**
     * Submit confirmed document uploads and/or form answers for the token's
     * identity. Authorized solely by [params]'s token; requires the
     * single-use `csrfToken` from [init].
     *
     * Recording a submission does not satisfy the requirement — see
     * [VerificationGateSubmitResult.documentsRecorded].
     */
    fun submit(params: VerificationGateSubmitParams): VerificationGateSubmitResult {
        val body = VerificationGateSubmitRequestWire(
            csrfToken = params.csrfToken,
            documents = params.documents?.map(::mapDocumentToWire),
            answers = params.answers?.map(::mapAnswerToWire)
        )
        val response = httpClient.post<VerificationGateSubmitResponseWire, VerificationGateSubmitRequestWire>(
            path = "/api/verification-gate/submit",
            body = body,
            headers = mapOf("Authorization" to "Bearer ${params.token}")
        )
        return VerificationGateSubmitResult(
            returnUrl = response.returnUrl ?: "",
            documentsRecorded = response.documents.size,
            answersRecorded = response.answers.size
        )
    }
}
