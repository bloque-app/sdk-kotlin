package app.bloque.sdk.orgs

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for organization operations
 */
class OrgsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a new organization
     *
     * @param params Organization parameters with profile and metadata
     * @return Created organization
     */
    fun create(params: CreateOrgParams): Organization {
        val profileWire = OrgProfileWire(
            legalName = params.profile.legalName,
            taxId = params.profile.taxId,
            incorporationDate = params.profile.incorporationDate,
            businessType = params.profile.businessType,
            incorporationCountryCode = params.profile.incorporationCountryCode,
            addressLine1 = params.profile.addressLine1,
            addressLine2 = params.profile.addressLine2,
            city = params.profile.city,
            state = params.profile.state,
            postalCode = params.profile.postalCode,
            country = params.profile.country,
            website = params.profile.website,
            email = params.profile.email,
            phone = params.profile.phone,
            industry = params.profile.industry
        )

        val request = CreateOrgRequestWire(
            orgType = "business",
            profile = profileWire,
            metadata = params.metadata
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateOrgResponseWire, CreateOrgRequestWire>(
            path = "/api/orgs",
            body = request,
            headers = headers
        )

        return mapOrganization(response.result.organization)
    }

    private fun mapOrganization(wire: OrganizationWire): Organization {
        val orgType = when (wire.orgType) {
            "business" -> OrgType.BUSINESS
            "individual" -> OrgType.INDIVIDUAL
            else -> OrgType.BUSINESS
        }

        val status = when (wire.status) {
            "awaiting_compliance_verification" -> OrgStatus.AWAITING_COMPLIANCE_VERIFICATION
            "active" -> OrgStatus.ACTIVE
            "suspended" -> OrgStatus.SUSPENDED
            "closed" -> OrgStatus.CLOSED
            else -> OrgStatus.AWAITING_COMPLIANCE_VERIFICATION
        }

        val profile = OrgProfile(
            legalName = wire.profile.legalName,
            taxId = wire.profile.taxId,
            incorporationDate = wire.profile.incorporationDate,
            businessType = wire.profile.businessType,
            incorporationCountryCode = wire.profile.incorporationCountryCode,
            addressLine1 = wire.profile.addressLine1,
            addressLine2 = wire.profile.addressLine2,
            city = wire.profile.city,
            state = wire.profile.state,
            postalCode = wire.profile.postalCode,
            country = wire.profile.country,
            website = wire.profile.website,
            email = wire.profile.email,
            phone = wire.profile.phone,
            industry = wire.profile.industry
        )

        return Organization(
            urn = wire.urn,
            orgType = orgType,
            profile = profile,
            metadata = wire.metadata,
            status = status
        )
    }
}
