package app.bloque.sdk.orgs

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for team operations.
 *
 * Note: there is no team-creation endpoint on the API - the domain interface
 * declares TeamService.create, but no controller route exposes it, so this
 * client intentionally has no create() method.
 */
class TeamsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * List teams in an organization
     *
     * @param orgUrn URN of the organization
     * @return List of teams
     */
    fun list(orgUrn: String): List<Team> {
        val response = httpClient.get<List<TeamWire>>(path = "/api/orgs/$orgUrn/teams")
        return response.map { it.toPublic() }
    }

    /**
     * Update team profile information, branding, and metadata
     *
     * @param params Team URN and fields to update (only non-null fields are sent)
     * @return Updated team
     */
    fun update(params: UpdateTeamParams): Team {
        val request = UpdateTeamRequestWire(
            name = params.name,
            imageUrl = params.imageUrl,
            description = params.description,
            metadata = params.metadata
        )

        val response = httpClient.patch<UpdateTeamResponseWire, UpdateTeamRequestWire>(
            path = "/api/teams/${params.teamUrn}",
            body = request
        )

        return response.result.team.toPublic()
    }

    /**
     * List members of a team, with their team-specific scopes and roles
     *
     * @param teamUrn URN of the team
     * @return List of team memberships
     */
    fun listMembers(teamUrn: String): List<TeamMember> {
        val response = httpClient.get<List<TeamMemberWire>>(path = "/api/teams/$teamUrn/members")
        return response.map { it.toPublic() }
    }

    /**
     * Remove a member from a team. Does not remove the member from the organization.
     *
     * @param params Team and member URNs
     */
    fun removeMember(params: RemoveTeamMemberParams) {
        httpClient.delete<SuccessResponseWire>(
            path = "/api/teams/${params.teamUrn}/members/${params.memberUrn}"
        )
    }

    /**
     * Update a team member's team-specific scopes, roles, and metadata
     *
     * @param params Team and member URNs plus fields to update (only non-null fields are sent)
     * @return Updated team membership
     */
    fun updateMember(params: UpdateTeamMemberParams): TeamMember {
        val request = UpdateTeamMemberRequestWire(
            teamScopes = params.teamScopes,
            teamRoles = params.teamRoles,
            metadata = params.metadata
        )

        val response = httpClient.patch<UpdateTeamMemberResponseWire, UpdateTeamMemberRequestWire>(
            path = "/api/teams/${params.teamUrn}/members/${params.memberUrn}",
            body = request
        )

        return response.result.teamMember.toPublic()
    }
}

private fun TeamWire.toPublic(): Team = Team(
    urn = urn,
    orgUrn = orgUrn,
    name = name,
    imageUrl = imageUrl,
    description = description,
    metadata = metadata
)

private fun TeamMemberWire.toPublic(): TeamMember = TeamMember(
    teamUrn = teamUrn,
    memberUrn = memberUrn,
    teamScopes = teamScopes,
    teamRoles = teamRoles,
    metadata = metadata
)
