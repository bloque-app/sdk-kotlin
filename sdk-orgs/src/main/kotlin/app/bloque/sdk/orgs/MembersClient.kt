package app.bloque.sdk.orgs

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for organization member operations.
 *
 * Note: role/scope management (creating/updating org-level roles) has no HTTP
 * controller on the API, so this client intentionally has no methods for it.
 */
class MembersClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * List members of an organization
     *
     * @param orgUrn URN of the organization
     * @return List of members
     */
    fun list(orgUrn: String): List<Member> {
        val response = httpClient.get<List<MemberWire>>(path = "/api/orgs/$orgUrn/members")
        return response.map { it.toPublic() }
    }

    /**
     * Update an organization member's profile and/or permissions
     *
     * @param params Member URN and fields to update (only non-null fields are sent)
     * @return Updated member
     */
    fun update(params: UpdateMemberParams): Member {
        val request = UpdateMemberRequestWire(
            title = params.title,
            displayName = params.displayName,
            isPublic = params.isPublic,
            orgScopes = params.orgScopes,
            orgRoles = params.orgRoles,
            metadata = params.metadata
        )

        val response = httpClient.patch<UpdateMemberResponseWire, UpdateMemberRequestWire>(
            path = "/api/members/${params.memberUrn}",
            body = request
        )

        return response.result.member.toPublic()
    }

    /**
     * Remove a member from an organization. Cascades to remove the member from
     * all teams within the organization.
     *
     * @param params Organization and member URNs
     */
    fun remove(params: RemoveMemberParams) {
        httpClient.delete<SuccessResponseWire>(
            path = "/api/orgs/${params.orgUrn}/members/${params.memberUrn}"
        )
    }
}

private fun MemberWire.toPublic(): Member = Member(
    urn = urn,
    orgUrn = orgUrn,
    isPublic = isPublic,
    title = title,
    displayName = displayName,
    identityUrn = identityUrn,
    orgScopes = orgScopes,
    orgRoles = orgRoles,
    metadata = metadata
)
