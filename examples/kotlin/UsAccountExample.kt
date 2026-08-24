package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.CreateAccountOptions
import app.bloque.sdk.accounts.CreateUsAccountParams
import app.bloque.sdk.accounts.UsAccountTosLinkParams
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile

/**
 * Kotlin example: US Account (Bridge-backed) creation flow
 *
 * A us-account requires the holder to accept Bridge's Terms of Service
 * before the account can be created. This example demonstrates the full
 * flow: request a ToS link, have the user accept it, then create the account
 * with the resulting `signed_agreement_id`.
 */
fun main() {
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "{your-origin-here}",
        originKey = "{your-origin-key-here}",
        mode = Mode.SANDBOX
    )

    val session = bloque.register(
        "example-user-us-account",
        IndividualRegisterParams(
            UserProfile(
                firstName = "Example",
                lastName = "User",
                email = "example-us-account@example.com",
                phone = "+14155551234"
            )
        )
    )

    // ============================================
    // Step 1: Request a Bridge Terms of Service link
    // ============================================
    println("=== Step 1: Get ToS Acceptance Link ===")

    val tosLink = session.accounts.usAccount.tosLink(
        UsAccountTosLinkParams(redirectUri = "https://myapp.com/onboarding/callback")
    )

    println("Open this URL for the user to accept Bridge's ToS:")
    println("  ${tosLink.url}")
    println("After acceptance, the redirect carries `signed_agreement_id` as a query parameter.")

    // ============================================
    // Step 2: Create the us-account with the signed agreement id
    // ============================================
    println("\n=== Step 2: Create US Account ===")

    // In a real app, `signedAgreementId` comes from the redirect_uri query
    // parameter after the user completes step 1 in a browser/webview.
    val signedAgreementId = "signed_agreement_id_from_redirect"

    val account = session.accounts.usAccount.create(
        CreateUsAccountParams(
            signedAgreementId = signedAgreementId,
            metadata = mapOf("source" to "example")
        ),
        CreateAccountOptions(waitLedger = true, timeout = 60000L)
    )

    println("Account URN: ${account.urn}")
    println("Status: ${account.status}")
    println("EVM deposit address: ${account.evmAddress}")
    println("Bank deposit instructions:")
    println("  Bank: ${account.bankName}")
    println("  Routing number: ${account.bankRoutingNumber}")
    println("  Account number: ${account.bankAccountNumber}")

    println("\n=== Example Completed ===")
}
