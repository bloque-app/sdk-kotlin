package app.bloque.sdk.orgs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ============================================
// Organization Types
// ============================================

/**
 * Organization type
 */
enum class OrgType {
    BUSINESS,
    DAO
}

/**
 * Organization status
 */
enum class OrgStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    AWAITING_COMPLIANCE_VERIFICATION
}

/**
 * An additional business location for an organization
 */
data class Place @JvmOverloads constructor(
    val countryCode: String,
    val state: String,
    val addressLine1: String,
    val postalCode: String,
    val city: String,
    val addressLine2: String? = null,
    val isPrimary: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Organization profile (business registration information)
 */
data class OrgProfile @JvmOverloads constructor(
    val legalName: String,
    val taxId: String,
    val incorporationDate: String,
    val businessType: String,
    val incorporationCountryCode: String,
    val incorporationState: String,
    val addressLine1: String,
    val postalCode: String,
    val city: String,
    val addressLine2: String? = null,
    val logoUrl: String? = null,
    val places: List<Place> = emptyList()
)

/**
 * Organization entity
 */
data class Organization @JvmOverloads constructor(
    val urn: String,
    val orgType: OrgType,
    val profile: OrgProfile,
    val status: OrgStatus,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Parameters for creating an organization
 */
data class CreateOrgParams @JvmOverloads constructor(
    val profile: OrgProfile,
    val orgType: OrgType = OrgType.BUSINESS,
    val metadata: Map<String, String> = emptyMap(),
    val idempotencyKey: String? = null
)

/**
 * Result of checking whether an organization slug is available
 */
data class SlugAvailability @JvmOverloads constructor(
    val available: Boolean,
    val normalizedSlug: String,
    val suggestions: List<String>? = null
)

// ============================================
// Member Types
// ============================================

/**
 * Member of an organization
 */
data class Member @JvmOverloads constructor(
    val urn: String,
    val orgUrn: String,
    val isPublic: Boolean,
    val title: String,
    val displayName: String,
    val identityUrn: String,
    val orgScopes: List<String> = emptyList(),
    val orgRoles: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Parameters for updating an organization member.
 * Only non-null fields are sent; all fields are optional.
 */
data class UpdateMemberParams @JvmOverloads constructor(
    val memberUrn: String,
    val title: String? = null,
    val displayName: String? = null,
    val isPublic: Boolean? = null,
    val orgScopes: List<String>? = null,
    val orgRoles: List<String>? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Parameters for removing a member from an organization (cascades to all teams)
 */
data class RemoveMemberParams(
    val orgUrn: String,
    val memberUrn: String
)

// ============================================
// Team Types
// ============================================

/**
 * Team within an organization
 */
data class Team @JvmOverloads constructor(
    val urn: String,
    val orgUrn: String,
    val name: String,
    val imageUrl: String,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Membership of a member within a team
 */
data class TeamMember @JvmOverloads constructor(
    val teamUrn: String,
    val memberUrn: String,
    val teamScopes: List<String> = emptyList(),
    val teamRoles: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Parameters for updating a team.
 * Only non-null fields are sent; all fields are optional.
 */
data class UpdateTeamParams @JvmOverloads constructor(
    val teamUrn: String,
    val name: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Parameters for updating a team member's team-scoped permissions.
 * Only non-null fields are sent; all fields are optional.
 */
data class UpdateTeamMemberParams @JvmOverloads constructor(
    val teamUrn: String,
    val memberUrn: String,
    val teamScopes: List<String>? = null,
    val teamRoles: List<String>? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Parameters for removing a member from a team (does not affect org membership)
 */
data class RemoveTeamMemberParams(
    val teamUrn: String,
    val memberUrn: String
)

// ============================================
// Invite Types
// ============================================

/**
 * Type of invitation
 */
enum class InviteType {
    MEMBER,
    TEAM
}

/**
 * Delivery channel for an invitation
 */
enum class InviteChannel {
    EMAIL,
    SMS,
    WHATSAPP,
    IDENTITY
}

/**
 * Current status of an invitation
 */
enum class InviteStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

/**
 * Information about the invitee for a member invitation
 */
data class MembershipInfo @JvmOverloads constructor(
    val title: String? = null,
    val name: String? = null,
    val email: String? = null,
    val payrollContractId: String? = null
)

/**
 * Details specific to a member invitation
 */
data class MemberInviteDetails @JvmOverloads constructor(
    val membershipInfo: MembershipInfo,
    val roles: List<String>,
    val scopes: List<String>,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Details specific to a team invitation
 */
data class TeamInviteDetails @JvmOverloads constructor(
    val teamUrn: String,
    val teamName: String,
    val message: String,
    val scopes: List<String>,
    val roles: List<String>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Invitation details, discriminated by invite type ([InviteType.MEMBER] vs [InviteType.TEAM]).
 * Used both when creating an invite and when reading one back from the API.
 */
sealed class InviteDetails {
    data class Member(val details: MemberInviteDetails) : InviteDetails()
    data class Team(val details: TeamInviteDetails) : InviteDetails()
}

/**
 * Channel-specific delivery target for an invitation.
 * Exactly one of [email], [phone], [identityUrn] is meaningful, matching [InviteChannel].
 */
data class InviteChannelRouting @JvmOverloads constructor(
    val email: String? = null,
    val phone: String? = null,
    val identityUrn: String? = null
) {
    companion object {
        @JvmStatic
        fun email(email: String): InviteChannelRouting = InviteChannelRouting(email = email)

        @JvmStatic
        fun phone(phone: String): InviteChannelRouting = InviteChannelRouting(phone = phone)

        @JvmStatic
        fun identity(identityUrn: String): InviteChannelRouting = InviteChannelRouting(identityUrn = identityUrn)
    }
}

/**
 * Basic organization info embedded in an invite
 */
data class OrgInfo @JvmOverloads constructor(
    val name: String,
    val logoUrl: String
)

/**
 * Information about who sent an invitation
 */
data class SenderInfo @JvmOverloads constructor(
    val identityUrn: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)

/**
 * An invitation to join an organization or team
 */
data class Invite @JvmOverloads constructor(
    val code: String,
    val orgUrn: String,
    val orgInfo: OrgInfo,
    val senderMemberUrn: String,
    val senderInfo: SenderInfo,
    val type: InviteType,
    val details: InviteDetails,
    val channel: InviteChannel,
    val channelRouting: InviteChannelRouting,
    val status: InviteStatus,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Parameters for creating an invitation
 */
data class CreateInviteParams @JvmOverloads constructor(
    val orgUrn: String,
    val details: InviteDetails,
    val channel: InviteChannel,
    val channelRouting: InviteChannelRouting,
    val metadata: Map<String, String> = emptyMap(),
    val idempotencyKey: String? = null
)

/**
 * Parameters for listing invitations
 */
data class ListInvitesParams @JvmOverloads constructor(
    val type: InviteType? = null,
    val status: InviteStatus? = null,
    val channel: InviteChannel? = null,
    val orgUrn: String? = null,
    val teamUrn: String? = null,
    val fromIdentityUrn: String? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val order: String? = null
)

/**
 * A page of invitations
 */
data class PagedInvites @JvmOverloads constructor(
    val data: List<Invite>,
    val total: Int
)

/**
 * Parameters for rejecting an invitation
 */
data class RejectInviteParams @JvmOverloads constructor(
    val code: String,
    val reason: String? = null,
    val message: String? = null,
    val metadata: Map<String, String>? = null
)

// ============================================
// Enum <-> wire value mapping (module-internal)
// ============================================

internal fun OrgType.wireValue(): String = when (this) {
    OrgType.BUSINESS -> "business"
    OrgType.DAO -> "dao"
}

internal fun String.toOrgType(): OrgType = when (this) {
    "dao" -> OrgType.DAO
    else -> OrgType.BUSINESS
}

internal fun OrgStatus.wireValue(): String = when (this) {
    OrgStatus.ACTIVE -> "active"
    OrgStatus.INACTIVE -> "inactive"
    OrgStatus.SUSPENDED -> "suspended"
    OrgStatus.AWAITING_COMPLIANCE_VERIFICATION -> "awaiting_compliance_verification"
}

internal fun String.toOrgStatus(): OrgStatus = when (this) {
    "active" -> OrgStatus.ACTIVE
    "inactive" -> OrgStatus.INACTIVE
    "suspended" -> OrgStatus.SUSPENDED
    else -> OrgStatus.AWAITING_COMPLIANCE_VERIFICATION
}

internal fun InviteType.wireValue(): String = when (this) {
    InviteType.MEMBER -> "member"
    InviteType.TEAM -> "team"
}

internal fun String.toInviteType(): InviteType = when (this) {
    "team" -> InviteType.TEAM
    else -> InviteType.MEMBER
}

internal fun InviteChannel.wireValue(): String = when (this) {
    InviteChannel.EMAIL -> "email"
    InviteChannel.SMS -> "sms"
    InviteChannel.WHATSAPP -> "whatsapp"
    InviteChannel.IDENTITY -> "identity"
}

internal fun String.toInviteChannel(): InviteChannel = when (this) {
    "sms" -> InviteChannel.SMS
    "whatsapp" -> InviteChannel.WHATSAPP
    "identity" -> InviteChannel.IDENTITY
    else -> InviteChannel.EMAIL
}

internal fun InviteStatus.wireValue(): String = when (this) {
    InviteStatus.PENDING -> "pending"
    InviteStatus.ACCEPTED -> "accepted"
    InviteStatus.REJECTED -> "rejected"
    InviteStatus.EXPIRED -> "expired"
}

internal fun String.toInviteStatus(): InviteStatus = when (this) {
    "accepted" -> InviteStatus.ACCEPTED
    "rejected" -> InviteStatus.REJECTED
    "expired" -> InviteStatus.EXPIRED
    else -> InviteStatus.PENDING
}

// ============================================
// Wire Types (Internal) - Organization
// ============================================

@Serializable
internal data class PlaceWire(
    @SerialName("country_code") val countryCode: String,
    val state: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("postal_code") val postalCode: String,
    val city: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class OrgProfileWire(
    @SerialName("legal_name") val legalName: String,
    @SerialName("tax_id") val taxId: String,
    @SerialName("incorporation_date") val incorporationDate: String,
    @SerialName("business_type") val businessType: String,
    @SerialName("incorporation_country_code") val incorporationCountryCode: String,
    @SerialName("incorporation_state") val incorporationState: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("postal_code") val postalCode: String,
    val city: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    val places: List<PlaceWire> = emptyList()
)

@Serializable
internal data class OrganizationWire(
    val urn: String,
    @SerialName("org_type") val orgType: String,
    val profile: OrgProfileWire,
    val status: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class CreateOrgRequestWire(
    @SerialName("org_type") val orgType: String,
    val profile: OrgProfileWire,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class CreateOrgResultWire(
    val organization: OrganizationWire
)

@Serializable
internal data class CreateOrgResponseWire(
    val result: CreateOrgResultWire
)

@Serializable
internal data class SlugAvailabilityWire(
    val available: Boolean,
    @SerialName("normalized_slug") val normalizedSlug: String,
    val suggestions: List<String>? = null
)

/**
 * Generic envelope for command responses whose result payload we don't need to read
 * (e.g. `{ "result": { "success": true }, "req_id": "..." }` or `{ "result": {} }`).
 */
@Serializable
internal data class SuccessResponseWire(
    val result: JsonElement? = null
)

// ============================================
// Wire Types (Internal) - Members
// ============================================

@Serializable
internal data class MemberWire(
    val urn: String,
    @SerialName("org_urn") val orgUrn: String,
    @SerialName("is_public") val isPublic: Boolean,
    val title: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("identity_urn") val identityUrn: String,
    @SerialName("org_scopes") val orgScopes: List<String> = emptyList(),
    @SerialName("org_roles") val orgRoles: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class UpdateMemberRequestWire(
    val title: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("org_scopes") val orgScopes: List<String>? = null,
    @SerialName("org_roles") val orgRoles: List<String>? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
internal data class UpdateMemberResultWire(
    val member: MemberWire
)

@Serializable
internal data class UpdateMemberResponseWire(
    val result: UpdateMemberResultWire
)

// ============================================
// Wire Types (Internal) - Teams
// ============================================

@Serializable
internal data class TeamWire(
    val urn: String,
    @SerialName("org_urn") val orgUrn: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class UpdateTeamRequestWire(
    val name: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val description: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
internal data class UpdateTeamResultWire(
    val team: TeamWire
)

@Serializable
internal data class UpdateTeamResponseWire(
    val result: UpdateTeamResultWire
)

@Serializable
internal data class TeamMemberWire(
    @SerialName("team_urn") val teamUrn: String,
    @SerialName("member_urn") val memberUrn: String,
    @SerialName("team_scopes") val teamScopes: List<String> = emptyList(),
    @SerialName("team_roles") val teamRoles: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class UpdateTeamMemberRequestWire(
    @SerialName("team_scopes") val teamScopes: List<String>? = null,
    @SerialName("team_roles") val teamRoles: List<String>? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
internal data class UpdateTeamMemberResultWire(
    @SerialName("team_member") val teamMember: TeamMemberWire
)

@Serializable
internal data class UpdateTeamMemberResponseWire(
    val result: UpdateTeamMemberResultWire
)

// ============================================
// Wire Types (Internal) - Invites
// ============================================

@Serializable
internal object EmptyRequestWire

@Serializable
internal data class OrgInfoWire(
    val name: String,
    @SerialName("logo_url") val logoUrl: String
)

@Serializable
internal data class SenderInfoWire(
    @SerialName("identity_urn") val identityUrn: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)

@Serializable
internal data class MembershipInfoWire(
    val title: String? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("payroll_contract_id") val payrollContractId: String? = null
)

@Serializable
internal data class MemberInviteDetailsWire(
    @SerialName("membership_info") val membershipInfo: MembershipInfoWire,
    val roles: List<String> = emptyList(),
    val scopes: List<String> = emptyList(),
    val message: String = "",
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class TeamInviteDetailsWire(
    @SerialName("team_urn") val teamUrn: String,
    @SerialName("team_name") val teamName: String,
    val message: String = "",
    val scopes: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class InviteChannelRoutingWire(
    val email: String? = null,
    val phone: String? = null,
    @SerialName("identity_urn") val identityUrn: String? = null
)

@Serializable
internal data class CreateMemberInviteRequestWire(
    val type: String = "member",
    val details: MemberInviteDetailsWire,
    val channel: String,
    @SerialName("channel_routing") val channelRouting: InviteChannelRoutingWire,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class CreateTeamInviteRequestWire(
    val type: String = "team",
    val details: TeamInviteDetailsWire,
    val channel: String,
    @SerialName("channel_routing") val channelRouting: InviteChannelRoutingWire,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class RejectInviteRequestWire(
    val reason: String? = null,
    val message: String? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Raw invite as returned by the API. [details] and [channelRouting] are kept as
 * untyped JSON here because their shape depends on [type] / [channel] respectively;
 * they're decoded into the right concrete wire type in InvitesClient.
 */
@Serializable
internal data class InviteWire(
    val code: String,
    @SerialName("org_urn") val orgUrn: String,
    @SerialName("org_info") val orgInfo: OrgInfoWire,
    @SerialName("sender_member_urn") val senderMemberUrn: String,
    @SerialName("sender_info") val senderInfo: SenderInfoWire,
    val type: String,
    val details: JsonElement,
    val channel: String,
    @SerialName("channel_routing") val channelRouting: JsonElement,
    val status: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class CreateInviteResultWire(
    val invite: InviteWire
)

@Serializable
internal data class CreateInviteResponseWire(
    val result: CreateInviteResultWire
)

@Serializable
internal data class ListInvitesResponseWire(
    val data: List<InviteWire>,
    val total: Int
)
