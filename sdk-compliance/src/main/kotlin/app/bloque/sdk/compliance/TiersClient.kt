package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

private fun mapRequirementFieldType(type: String): RequirementFieldType = when (type) {
    "text" -> RequirementFieldType.TEXT
    "number" -> RequirementFieldType.NUMBER
    "date" -> RequirementFieldType.DATE
    "select" -> RequirementFieldType.SELECT
    "boolean" -> RequirementFieldType.BOOLEAN
    else -> RequirementFieldType.TEXT
}

internal fun mapRequirementField(wire: RequirementFieldWire): RequirementField {
    return RequirementField(
        key = wire.key,
        label = wire.label,
        type = mapRequirementFieldType(wire.type),
        required = wire.required,
        options = wire.options
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
        fields = wire.fields?.map(::mapRequirementField),
        submittedAt = wire.submittedAt
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
 * Tier status client — the read side of the compliance engine's
 * verification-tier control plane.
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
            verificationFlow = mapVerificationFlow(response.verificationFlow)
        )
    }
}
