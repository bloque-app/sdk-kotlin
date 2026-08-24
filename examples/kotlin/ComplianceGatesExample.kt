package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.compliance.*
import app.bloque.sdk.core.BloqueVerificationPendingError
import app.bloque.sdk.core.BloqueVerificationRequiredError
import app.bloque.sdk.core.Mode
import app.bloque.sdk.UpdateOriginMetadataParams
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile

/**
 * Kotlin example: read an identity's compliance tier status and drive the
 * hosted TOS / verification gates.
 *
 * This demonstrates:
 * - `session.compliance.tiers.getStatus()` — what's missing, what's already
 *   under review, and which hosted gate resolves the gap. Includes
 *   `nextRecomputeAt`, a polling-backoff hint.
 * - `session.compliance.tosGate.start()` — mint a Level 0 TOS acceptance link,
 *   then `init()`/`challenge()`/`accept()` for a native client rendering its
 *   own TOS screen: `developerName`/`passkeyRequired` branding/gating, and
 *   accepting with either a pre-built device attestation or the raw
 *   WebAuthn passkey parts (`TosGateAttestation`'s sealed choice).
 * - `session.compliance.verificationGate.start()` — mint a hosted
 *   document/form submission link (return_url is optional here), then
 *   `init()` to render its requirement cards: per-field help text
 *   (`description`), localized `select` options, and `requiresUpload`
 *   branching (a form-only requirement never shows a file picker).
 * - Handling `pendingRequirements` distinctly from actionable ones — never
 *   re-collect what a reviewer already has.
 * - Catching `BloqueVerificationRequiredError` / `BloqueVerificationPendingError`
 *   thrown by other SDK calls once a tier limit blocks an action.
 * - `bloque.origins.updateMetadata()` — self-service `gate_accent_color` /
 *   `verification_gate_return_url_allowlist` config, called on the root
 *   SDK instance (sibling to `connect()`/`register()`) since it needs no
 *   connected session.
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
    // Nothing time-driven changes this status before nextRecomputeAt (a TOS
    // grace deadline or an evidence expiry) — a cheap hint for how long a
    // polling loop can safely back off, distinct from "poll again once the
    // user tells you they finished the hosted gate".
    status.nextRecomputeAt?.let { println("Next automatic recompute at: $it") }

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

            // Usually you'd just open `gate.url` in a browser and let the
            // hosted page drive init()/challenge()/accept() itself. Shown
            // here for a native client that renders its own TOS screen
            // instead of embedding the hosted page.
            val tosInit = session.compliance.tosGate.init(TosGateInitParams(token = gate.token))
            println("Developer name for branding: ${tosInit.developerName}")

            // Accepting can optionally hand control of the identity's
            // PassAccount to a device — either a pre-built device
            // attestation, or the raw WebAuthn registration parts for the
            // server to frame itself. `passkeyRequired` gates whether this
            // step applies at all.
            val attestation = if (tosInit.passkeyRequired) {
                val challengeResult = session.compliance.tosGate.challenge(TosGateInitParams(token = gate.token))
                challengeResult.passkey?.let { passkeyChallenge ->
                    println("Passkey challenge context: ${passkeyChallenge.context}")
                    // A real client would hand `passkeyChallenge` to the
                    // platform's WebAuthn/authenticator API here and build
                    // this from the resulting credential.
                    TosGateAttestation.Passkey(
                        TosGatePasskeyRegistration(
                            credentialId = "base64url-credential-id",
                            authenticatorData = "base64url-authenticator-data",
                            clientData = "base64url-client-data",
                            publicKey = "base64url-public-key",
                            context = passkeyChallenge.context
                        )
                    )
                }
            } else {
                null
            }

            val accepted = session.compliance.tosGate.accept(
                TosGateAcceptParams(
                    token = gate.token,
                    csrfToken = tosInit.csrfToken,
                    attestation = attestation
                )
            )
            println("Accepted at: ${accepted.acceptance.acceptedAt}")
        }
        VerificationFlowType.DOCUMENT_SUBMISSION -> {
            // return_url is optional for the verification gate — omit it
            // when there's nowhere meaningful to redirect back to.
            val gate = session.compliance.verificationGate.start(StartVerificationGateParams())
            println("Open this URL to submit documents: ${gate.url}")

            // init() renders the actual requirement cards. Demonstrates
            // per-field help text, localized select options, and the
            // requires_upload opt-out (a form-only manual_review must never
            // show a file picker even though its `kind` is normally
            // uploadable).
            val initResult = session.compliance.verificationGate.init(
                VerificationGateInitParams(token = gate.token)
            )
            initResult.accentColor?.let { println("Brand accent color: $it") }

            for (requirement in initResult.requirements) {
                println("- ${requirement.title ?: requirement.key} (${requirement.key})")
                requirement.description?.let { println("  ${it}") }
                if (requirement.uploadable) {
                    println("  Accepts a document upload (${requirement.uploadIntents?.size ?: 0} content types).")
                }
                for (field in requirement.fields.orEmpty()) {
                    println("  Field '${field.label}'${if (field.required == true) " (required)" else ""}")
                    field.description?.let { println("    ${it}") }
                    when (field.type) {
                        RequirementFieldType.SELECT -> {
                            // Options can be legacy plain strings (label ==
                            // null, render `value` as-is) or localized —
                            // pick the field's pinned `locale`, falling back
                            // to the user's own language preference.
                            val lang = field.locale ?: "en"
                            for (option in field.options.orEmpty()) {
                                val label = option.label
                                val displayLabel = when {
                                    label == null -> option.value
                                    lang == "es" -> label.es
                                    else -> label.en
                                }
                                println("    option: ${option.value} -> $displayLabel")
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Requirements already submitted and awaiting a reviewer are
            // reported separately — show them read-only, never re-collect.
            for (pending in initResult.pendingRequirements) {
                println("- (under review) ${pending.title ?: pending.key}, submitted ${pending.submittedAt}")
            }
        }
        null -> println("Nothing actionable — either fully verified or waiting on a reviewer.")
    }

    // ============================================
    // Example 3: Self-service origin configuration
    // ============================================
    println("\n=== Example 3: Self-Service Gate Personalization ===")

    // No session required — authenticated purely by the origin's own
    // api_key. Called on `bloque` (the root SDK instance), not `session`.
    // originName/apiKey default to the SDK's own config, so they can be
    // omitted here since we built `bloque` with them above.
    // Typically run once from a deploy script, not per-request.
    val metadataResult = bloque.origins.updateMetadata(
        UpdateOriginMetadataParams(
            gateAccentColor = "#1a73e8",
            verificationGateReturnUrlAllowlist = listOf("https://myapp.example.com/verification-complete")
        )
    )
    println("Origin metadata updated: ${metadataResult.updated}")

    // ============================================
    // Example 4: Handle verification errors from other calls
    // ============================================
    println("\n=== Example 4: Handling Verification Errors ===")

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
        // Deliberately no getVerificationLink() here — opening a gate would
        // ask the user to resubmit what a reviewer already has. Show an
        // "under review" state and retry the original action later; it
        // succeeds once the review lands.
        println("Already submitted — nothing to do but retry later.")
        println("Pending: ${e.pendingRequirements}")
    }

    println("\n✅ Compliance gates example completed!")
}
