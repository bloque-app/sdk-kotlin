package app.bloque.sdk.orgs

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.BloqueSerializationException
import kotlinx.serialization.json.JsonElement

/**
 * Main client for organization operations.
 *
 * Exposes nested clients for related resources, mirroring the AccountsClient
 * aggregator pattern:
 * - [teams] for team management
 * - [invites] for invitation management
 * - [members] for organization member management
 */
class OrgsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Team operations
     */
    val teams: TeamsClient = TeamsClient(httpClient)

    /**
     * Invitation operations
     */
    val invites: InvitesClient = InvitesClient(httpClient)

    /**
     * Organization member operations
     */
    val members: MembersClient = MembersClient(httpClient)

    /**
     * Create a new organization
     *
     * The creator automatically becomes the first member with full administrative
     * permissions. New organizations start with status AWAITING_COMPLIANCE_VERIFICATION.
     *
     * @param params Organization parameters with type, profile, and metadata
     * @return Created organization
     */
    fun create(params: CreateOrgParams): Organization {
        val profileWire = OrgProfileWire(
            legalName = params.profile.legalName,
            taxId = params.profile.taxId,
            incorporationDate = params.profile.incorporationDate,
            businessType = params.profile.businessType,
            incorporationCountryCode = params.profile.incorporationCountryCode,
            incorporationState = params.profile.incorporationState,
            addressLine1 = params.profile.addressLine1,
            addressLine2 = params.profile.addressLine2,
            postalCode = params.profile.postalCode,
            city = params.profile.city,
            logoUrl = params.profile.logoUrl,
            places = params.profile.places.map { it.toWire() }
        )

        val request = CreateOrgRequestWire(
            orgType = params.orgType.wireValue(),
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

    /**
     * Get an organization by URN
     *
     * @param orgUrn URN of the organization
     * @return The organization
     */
    fun get(orgUrn: String): Organization {
        val response = httpClient.get<OrganizationWire>(path = "/api/orgs/$orgUrn")
        return mapOrganization(response)
    }

    /**
     * Check whether an organization slug is available. Public endpoint - no
     * authentication required.
     *
     * @param slug Slug to check
     * @return Availability info, including suggestions if the slug is taken
     */
    fun verifySlug(slug: String): SlugAvailability {
        val response = httpClient.get<SlugAvailabilityWire>(path = "/api/orgs/verify-slug?slug=$slug")
        return SlugAvailability(
            available = response.available,
            normalizedSlug = response.normalizedSlug,
            suggestions = response.suggestions
        )
    }

    /**
     * List all organizations where the current user is a member
     *
     * @return List of organizations
     */
    fun listMine(): List<Organization> {
        val response = httpClient.get<List<OrganizationWire>>(path = "/api/identities/me/orgs")
        return response.map { mapOrganization(it) }
    }

    /**
     * Permanently delete an organization and all its associated data (members,
     * teams, roles, invitations). This is irreversible.
     *
     * @param orgUrn URN of the organization to delete
     */
    fun delete(orgUrn: String) {
        try {
            httpClient.delete<JsonElement>(path = "/api/orgs/$orgUrn")
        } catch (e: BloqueSerializationException) {
            // The API returns 204 No Content on success. BloqueHttpClient treats an
            // empty response body as a serialization failure regardless of status
            // code, so a clean 204 surfaces here as this exception - treat it as success.
            if (e.message?.contains("Empty response body") != true) throw e
        }
    }

    private fun mapOrganization(wire: OrganizationWire): Organization {
        return Organization(
            urn = wire.urn,
            orgType = wire.orgType.toOrgType(),
            profile = wire.profile.toPublic(),
            status = wire.status.toOrgStatus(),
            metadata = wire.metadata
        )
    }
}

private fun Place.toWire(): PlaceWire = PlaceWire(
    countryCode = countryCode,
    state = state,
    addressLine1 = addressLine1,
    postalCode = postalCode,
    city = city,
    addressLine2 = addressLine2,
    isPrimary = isPrimary,
    metadata = metadata
)

private fun PlaceWire.toPublic(): Place = Place(
    countryCode = countryCode,
    state = state,
    addressLine1 = addressLine1,
    postalCode = postalCode,
    city = city,
    addressLine2 = addressLine2,
    isPrimary = isPrimary,
    metadata = metadata
)

private fun OrgProfileWire.toPublic(): OrgProfile = OrgProfile(
    legalName = legalName,
    taxId = taxId,
    incorporationDate = incorporationDate,
    businessType = businessType,
    incorporationCountryCode = incorporationCountryCode,
    incorporationState = incorporationState,
    addressLine1 = addressLine1,
    postalCode = postalCode,
    city = city,
    addressLine2 = addressLine2,
    logoUrl = logoUrl,
    places = places.map { it.toPublic() }
)
