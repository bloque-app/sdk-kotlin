package app.bloque.sdk.orgs

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Client for invitation operations.
 *
 * Invitations are discriminated by [InviteType]: a MEMBER invite carries
 * [MemberInviteDetails], a TEAM invite carries [TeamInviteDetails]. The API
 * represents this as a `type` field alongside a `details` object whose shape
 * depends on `type`. This client models that as the [InviteDetails] sealed
 * class so callers get compile-time exhaustiveness (a `when` over
 * InviteDetails.Member / InviteDetails.Team) instead of an untyped map, while
 * decoding responses by branching on the wire `type` string.
 */
class InvitesClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    // Used only to decode the untyped `details` / `channel_routing` JSON fragments
    // in InviteWire into their concrete wire type once we know `type` / `channel`.
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create an invitation for a user to join an organization (member invite) or
     * a specific team (team invite)
     *
     * @param params Invite parameters, including the discriminated [InviteDetails]
     * @return The created invite
     */
    fun create(params: CreateInviteParams): Invite {
        val channelValue = params.channel.wireValue()
        val routingWire = InviteChannelRoutingWire(
            email = params.channelRouting.email,
            phone = params.channelRouting.phone,
            identityUrn = params.channelRouting.identityUrn
        )
        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = when (val details = params.details) {
            is InviteDetails.Member -> httpClient.post<CreateInviteResponseWire, CreateMemberInviteRequestWire>(
                path = "/api/orgs/${params.orgUrn}/invite",
                body = CreateMemberInviteRequestWire(
                    details = MemberInviteDetailsWire(
                        membershipInfo = MembershipInfoWire(
                            title = details.details.membershipInfo.title,
                            name = details.details.membershipInfo.name,
                            email = details.details.membershipInfo.email,
                            payrollContractId = details.details.membershipInfo.payrollContractId
                        ),
                        roles = details.details.roles,
                        scopes = details.details.scopes,
                        message = details.details.message,
                        metadata = details.details.metadata
                    ),
                    channel = channelValue,
                    channelRouting = routingWire,
                    metadata = params.metadata
                ),
                headers = headers
            )

            is InviteDetails.Team -> httpClient.post<CreateInviteResponseWire, CreateTeamInviteRequestWire>(
                path = "/api/orgs/${params.orgUrn}/invite",
                body = CreateTeamInviteRequestWire(
                    details = TeamInviteDetailsWire(
                        teamUrn = details.details.teamUrn,
                        teamName = details.details.teamName,
                        message = details.details.message,
                        scopes = details.details.scopes,
                        roles = details.details.roles,
                        metadata = details.details.metadata
                    ),
                    channel = channelValue,
                    channelRouting = routingWire,
                    metadata = params.metadata
                ),
                headers = headers
            )
        }

        return mapInvite(response.result.invite)
    }

    /**
     * Get an invitation by its code. Public endpoint - no authentication required.
     *
     * @param code 8-character invitation code
     * @return The invite
     */
    fun get(code: String): Invite {
        val response = httpClient.get<InviteWire>(path = "/api/invite/$code")
        return mapInvite(response)
    }

    /**
     * List invitations matching the given filters
     *
     * @param params Filter and pagination parameters
     * @return Page of invites with total count
     */
    @JvmOverloads
    fun list(params: ListInvitesParams = ListInvitesParams()): PagedInvites {
        val queryParams = buildString {
            val parts = mutableListOf<String>()
            params.type?.let { parts.add("type=${it.wireValue()}") }
            params.status?.let { parts.add("status=${it.wireValue()}") }
            params.channel?.let { parts.add("channel=${it.wireValue()}") }
            params.orgUrn?.let { parts.add("org_urn=$it") }
            params.teamUrn?.let { parts.add("team_urn=$it") }
            params.fromIdentityUrn?.let { parts.add("from_identity_urn=$it") }
            params.limit?.let { parts.add("limit=$it") }
            params.offset?.let { parts.add("offset=$it") }
            params.order?.let { parts.add("order=$it") }
            if (parts.isNotEmpty()) {
                append("?")
                append(parts.joinToString("&"))
            }
        }

        val response = httpClient.get<ListInvitesResponseWire>(path = "/api/invites$queryParams")

        return PagedInvites(
            data = response.data.map { mapInvite(it) },
            total = response.total
        )
    }

    /**
     * Accept an invitation, creating the corresponding member/team-member records
     *
     * @param code 8-character invitation code
     * @return The accepted invite
     */
    fun accept(code: String): Invite {
        val response = httpClient.post<CreateInviteResponseWire, EmptyRequestWire>(
            path = "/api/invite/$code/accept",
            body = EmptyRequestWire
        )
        return mapInvite(response.result.invite)
    }

    /**
     * Reject an invitation
     *
     * @param params Code plus optional reason/message/metadata about the rejection
     * @return The rejected invite
     */
    fun reject(params: RejectInviteParams): Invite {
        val response = httpClient.post<CreateInviteResponseWire, RejectInviteRequestWire>(
            path = "/api/invite/${params.code}/reject",
            body = RejectInviteRequestWire(
                reason = params.reason,
                message = params.message,
                metadata = params.metadata
            )
        )
        return mapInvite(response.result.invite)
    }

    /**
     * Resend a pending invitation through its original channel
     *
     * @param code 8-character invitation code
     * @return The resent invite
     */
    fun resend(code: String): Invite {
        val response = httpClient.post<CreateInviteResponseWire, EmptyRequestWire>(
            path = "/api/invite/$code/resend",
            body = EmptyRequestWire
        )
        return mapInvite(response.result.invite)
    }

    private fun mapInvite(wire: InviteWire): Invite {
        val type = wire.type.toInviteType()

        val details: InviteDetails = when (type) {
            InviteType.MEMBER -> {
                val detailsWire = json.decodeFromJsonElement<MemberInviteDetailsWire>(wire.details)
                InviteDetails.Member(
                    MemberInviteDetails(
                        membershipInfo = MembershipInfo(
                            title = detailsWire.membershipInfo.title,
                            name = detailsWire.membershipInfo.name,
                            email = detailsWire.membershipInfo.email,
                            payrollContractId = detailsWire.membershipInfo.payrollContractId
                        ),
                        roles = detailsWire.roles,
                        scopes = detailsWire.scopes,
                        message = detailsWire.message,
                        metadata = detailsWire.metadata
                    )
                )
            }

            InviteType.TEAM -> {
                val detailsWire = json.decodeFromJsonElement<TeamInviteDetailsWire>(wire.details)
                InviteDetails.Team(
                    TeamInviteDetails(
                        teamUrn = detailsWire.teamUrn,
                        teamName = detailsWire.teamName,
                        message = detailsWire.message,
                        scopes = detailsWire.scopes,
                        roles = detailsWire.roles,
                        metadata = detailsWire.metadata
                    )
                )
            }
        }

        val routingWire = json.decodeFromJsonElement<InviteChannelRoutingWire>(wire.channelRouting)

        return Invite(
            code = wire.code,
            orgUrn = wire.orgUrn,
            orgInfo = OrgInfo(name = wire.orgInfo.name, logoUrl = wire.orgInfo.logoUrl),
            senderMemberUrn = wire.senderMemberUrn,
            senderInfo = SenderInfo(
                identityUrn = wire.senderInfo.identityUrn,
                name = wire.senderInfo.name,
                email = wire.senderInfo.email,
                phone = wire.senderInfo.phone
            ),
            type = type,
            details = details,
            channel = wire.channel.toInviteChannel(),
            channelRouting = InviteChannelRouting(
                email = routingWire.email,
                phone = routingWire.phone,
                identityUrn = routingWire.identityUrn
            ),
            status = wire.status.toInviteStatus(),
            metadata = wire.metadata
        )
    }
}
