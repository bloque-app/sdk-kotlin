package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile
import app.bloque.sdk.orgs.CreateInviteParams
import app.bloque.sdk.orgs.CreateOrgParams
import app.bloque.sdk.orgs.InviteChannel
import app.bloque.sdk.orgs.InviteChannelRouting
import app.bloque.sdk.orgs.InviteDetails
import app.bloque.sdk.orgs.MemberInviteDetails
import app.bloque.sdk.orgs.MembershipInfo
import app.bloque.sdk.orgs.OrgProfile
import app.bloque.sdk.orgs.OrgType

/**
 * Kotlin example: Organization, invite, and team operations
 *
 * This example demonstrates how to create an organization, look it up, list the
 * caller's organizations, invite a member, and list teams using the Bloque SDK.
 */
fun main() {
    // Initialize the SDK
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "{your-origin-here}",
        originKey = "{your-origin-key-here}",
        mode = Mode.SANDBOX
    )

    // Connect to a user session
    val session = bloque.register("example-user", IndividualRegisterParams(UserProfile(
        firstName = "Example",
        lastName = "User",
        email = "example@example.com",
        phone = "+1234567890"
    )))

    // ============================================
    // Example 1: Create an organization
    // ============================================
    println("=== Example 1: Create Organization ===")

    val org = session.orgs.create(
        CreateOrgParams(
            profile = OrgProfile(
                legalName = "Test Business Inc.",
                taxId = "123456789",
                incorporationDate = "2023-01-01",
                businessType = "LLC",
                incorporationCountryCode = "US",
                incorporationState = "CA",
                addressLine1 = "123 Business St",
                postalCode = "90210",
                city = "Los Angeles"
            ),
            orgType = OrgType.BUSINESS,
            metadata = mapOf("industry" to "Technology")
        )
    )

    println("Organization URN: ${org.urn}")
    println("Status: ${org.status}")
    println("Legal name: ${org.profile.legalName}")

    // ============================================
    // Example 2: Get organization by URN
    // ============================================
    println("\n=== Example 2: Get Organization ===")

    val fetchedOrg = session.orgs.get(org.urn)
    println("Fetched org: ${fetchedOrg.profile.legalName} (${fetchedOrg.status})")

    // ============================================
    // Example 3: List the current user's organizations
    // ============================================
    println("\n=== Example 3: List My Organizations ===")

    val myOrgs = session.orgs.listMine()
    println("Total organizations: ${myOrgs.size}")
    myOrgs.forEach { println("  - ${it.urn} (${it.profile.legalName})") }

    // ============================================
    // Example 4: Invite a new member via email
    // ============================================
    println("\n=== Example 4: Create Member Invite ===")

    val invite = session.orgs.invites.create(
        CreateInviteParams(
            orgUrn = org.urn,
            details = InviteDetails.Member(
                MemberInviteDetails(
                    membershipInfo = MembershipInfo(
                        title = "Senior Developer",
                        name = "Jane Smith",
                        email = "jane@acme.com"
                    ),
                    roles = listOf("developer"),
                    scopes = listOf("orgs.read", "team.read", "member.read.self"),
                    message = "Welcome to our team!"
                )
            ),
            channel = InviteChannel.EMAIL,
            channelRouting = InviteChannelRouting.email("jane@acme.com")
        )
    )

    println("Invite code: ${invite.code}")
    println("Invite status: ${invite.status}")

    // ============================================
    // Example 5: List teams in the organization
    // ============================================
    println("\n=== Example 5: List Teams ===")

    val teams = session.orgs.teams.list(org.urn)
    println("Total teams: ${teams.size}")
    teams.forEach { println("  - ${it.urn} (${it.name})") }

    println("\n=== All Examples Completed ===")
}
