package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.compliance.*
import app.bloque.sdk.core.BloqueVerificationPendingError
import app.bloque.sdk.core.BloqueVerificationRequiredError
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile

/**
 * Kotlin example: read an identity's compliance tier status and drive the
 * hosted TOS / verification gates.
 *
 * This demonstrates:
 * - `session.compliance.tiers.getStatus()` — what's missing, what's already
 *   under review, and which hosted gate resolves the gap.
 * - `session.compliance.tosGate.start()` — mint a Level 0 TOS acceptance link.
 * - `session.compliance.verificationGate.start()` — mint a hosted
 *   document/form submission link (return_url is optional here).
 * - Catching `BloqueVerificationRequiredError` / `BloqueVerificationPendingError`
 *   thrown by other SDK calls once a tier limit blocks an action.
 */
fun main() {
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "origin",
        originKey = "your-origin-key",
        mode = Mode.SANDBOX
    )

    val session = bloque.register(
        alias = "compliance-example-user",
        params = IndividualRegisterParams(
            UserProfile(
                firstName = "Ada",
                lastName = "Lovelace",
                birthdate = "1990-01-01",
                email = "ada@example.com",
                phone = "+1234567890",
                nationality = "USA",
                countryOfResidence = "USA",
                country = "USA"
            )
        )
    )
    val urn = requireNotNull(session.getUrn()) { "Registration did not return a URN" }

    // ============================================
    // Example 1: Read tier status
    // ============================================
    println("=== Example 1: Tier Status ===")

    val status = session.compliance.tiers.getStatus(GetTierStatusParams(urn = urn))
    println("Effective level: ${status.effectiveLevel}")
    println("Next level: ${status.nextLevel}")
    println("Missing requirements: ${status.missingRequirements}")
    println("Pending (already submitted, under review): ${status.pendingRequirements}")

    // ============================================
    // Example 2: Follow the hosted-gate handoff
    // ============================================
    println("\n=== Example 2: Hosted Gate Handoff ===")

    when (status.verificationFlow?.type) {
        VerificationFlowType.TOS_HOSTED_ACCEPTANCE -> {
            val gate = session.compliance.tosGate.start(
                StartTosGateParams(returnUrl = "https://myapp.example.com/verification-complete")
            )
            println("Open this URL to accept the TOS: ${gate.url}")
        }
        VerificationFlowType.DOCUMENT_SUBMISSION -> {
            // return_url is optional for the verification gate — omit it
            // when there's nowhere meaningful to redirect back to.
            val gate = session.compliance.verificationGate.start(StartVerificationGateParams())
            println("Open this URL to submit documents: ${gate.url}")
        }
        null -> println("Nothing actionable — either fully verified or waiting on a reviewer.")
    }

    // ============================================
    // Example 3: Handle verification errors from other calls
    // ============================================
    println("\n=== Example 3: Handling Verification Errors ===")

    try {
        // Any gated SDK call (transfers, swaps, card authorizations, ...)
        // can throw one of these once the compliance engine steps in.
        session.compliance.tiers.getStatus(GetTierStatusParams(urn = urn))
    } catch (e: BloqueVerificationRequiredError) {
        println("Verification required — reason: ${e.reason}")
        println("Missing: ${e.missingRequirements}, pending: ${e.pendingRequirements}")
        val link = e.getVerificationLink(returnUrl = "https://myapp.example.com/verification-complete")
        if (link != null) {
            println("Send your user to: ${link.url}")
        } else {
            println("No hosted-page handoff for this gap (likely KYC).")
        }
    } catch (e: BloqueVerificationPendingError) {
        println("Already submitted — nothing to do but retry later.")
        println("Pending: ${e.pendingRequirements}")
    }

    println("\n✅ Compliance gates example completed!")
}
