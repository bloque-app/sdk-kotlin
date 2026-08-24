package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

private fun mapVerificationCheck(wire: ComplianceVerificationCheckWire): ComplianceVerificationCheck {
    return ComplianceVerificationCheck(
        verified = wire.verified,
        comment = wire.comment,
        declineReasons = wire.declineReasons
    )
}

private fun mapDatabaseScreeningCheck(wire: ComplianceDatabaseScreeningCheckWire): ComplianceDatabaseScreeningCheck {
    return ComplianceDatabaseScreeningCheck(
        verified = wire.verified,
        comment = wire.comment,
        declineReasons = wire.declineReasons,
        databases = wire.databases
    )
}

private fun mapVerificationResult(wire: ComplianceVerificationResultWire?): ComplianceVerificationResult? {
    if (wire == null) return null
    return ComplianceVerificationResult(
        verified = wire.verified,
        providerReference = wire.providerReference,
        checks = ComplianceVerificationChecks(
            profile = mapVerificationCheck(wire.checks.profile),
            document = mapVerificationCheck(wire.checks.document),
            facial = wire.checks.facial?.let(::mapVerificationCheck),
            databaseScreening = mapDatabaseScreeningCheck(wire.checks.databaseScreening),
            adverseMedia = wire.checks.adverseMedia?.let(::mapVerificationCheck)
        ),
        applicant = ComplianceApplicant(
            firstName = wire.applicant.firstName,
            lastName = wire.applicant.lastName,
            dob = wire.applicant.dob,
            gender = wire.applicant.gender,
            nationality = wire.applicant.nationality,
            residenceCountry = wire.applicant.residenceCountry,
            email = wire.applicant.email,
            phone = wire.applicant.phone
        ),
        documents = wire.documents.map { doc ->
            ComplianceVerificationDocument(
                type = doc.type,
                mappedType = doc.mappedType,
                documentNumber = doc.documentNumber,
                issueDate = doc.issueDate,
                expiryDate = doc.expiryDate,
                issuingAuthority = doc.issuingAuthority,
                status = doc.status
            )
        },
        clientInfo = wire.clientInfo?.let {
            ComplianceClientInfo(ip = it.ip, countryCode = it.countryCode, city = it.city)
        },
        faceMatchConfidence = wire.faceMatchConfidence,
        declineReasons = wire.declineReasons
    )
}

private fun mapKycDocument(wire: KycDocumentWire): KycDocument {
    return KycDocument(
        documentType = wire.documentType,
        side = wire.side,
        imageS3Key = wire.imageS3Key,
        imageSizeBytes = wire.imageSizeBytes,
        downloadUrl = wire.downloadUrl
    )
}

private fun mapKycStatus(status: String): KycVerificationStatus = when (status) {
    "awaiting_compliance_verification" -> KycVerificationStatus.AWAITING_VERIFICATION
    "approved" -> KycVerificationStatus.APPROVED
    "rejected" -> KycVerificationStatus.REJECTED
    else -> KycVerificationStatus.AWAITING_VERIFICATION
}

/**
 * Client for KYC (Know Your Customer) operations
 */
class KycClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Start a KYC verification process
     *
     * @param params Verification parameters — see [KycVerificationParams] for
     * a note on which fields are actually honored by the API today
     * @return KYC verification response with status and verification URL
     */
    fun startVerification(params: KycVerificationParams): KycVerificationResponse {
        val request = KycVerificationRequestWire(
            urn = params.urn,
            type = params.type
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<KycVerificationResponseDirect, KycVerificationRequestWire>(
            path = "/api/compliance",
            body = request,
            headers = headers
        )

        return mapStartResponse(response)
    }

    /**
     * Get the status of a KYC verification
     *
     * @param params Parameters with URN
     * @return KYC verification response with current status
     */
    fun getVerification(params: GetKycVerificationParams): KycVerificationResponse {
        val response = httpClient.get<KycVerificationResponseDirect>(
            path = "/api/compliance/${params.urn}"
        )

        return mapGetResponse(response)
    }

    /**
     * Fetch document images (ID front/back, selfie, etc.) on file for a
     * user's latest KYC verification, with presigned download URLs where
     * available.
     *
     * @param params Parameters with URN
     * @return document images and their overall download status
     */
    fun getDocuments(params: GetKycVerificationParams): KycDocumentsResult {
        val response = httpClient.get<KycDocumentsResponseWire>(
            path = "/api/compliance/${params.urn}/documents"
        )

        return KycDocumentsResult(
            documentsStatus = response.documentsStatus,
            documents = response.documents.map(::mapKycDocument)
        )
    }

    private fun mapStartResponse(wire: KycVerificationResponseDirect): KycVerificationResponse {
        return KycVerificationResponse(
            status = mapKycStatus(wire.status),
            url = wire.url ?: "",
            completedAt = null,
            result = mapVerificationResult(wire.result),
            documentsStatus = wire.documentsStatus
        )
    }

    private fun mapGetResponse(wire: KycVerificationResponseDirect): KycVerificationResponse {
        return KycVerificationResponse(
            status = mapKycStatus(wire.status),
            url = wire.verificationUrl ?: wire.url ?: "",
            completedAt = wire.completedAt,
            result = mapVerificationResult(wire.result),
            documentsStatus = wire.documentsStatus
        )
    }
}
