package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.CreateCardAccountParams
import app.bloque.sdk.accounts.CreateVirtualAccountParams
import app.bloque.sdk.accounts.CreateAccountOptions
import app.bloque.sdk.core.Mode

/**
 * Example: Creating multiple accounts linked to the same ledger
 *
 * This demonstrates how to:
 * 1. Create a virtual account (which gets assigned a ledger)
 * 2. Create a card account linked to the same ledger
 *
 * Both accounts will share the same balance through the shared ledger.
 */
fun main() {
    // Initialize SDK
    val bloque = BloqueSDK.create(
        origin = "my-app-origin",
        apiKey = "sk_test_your_api_key_here",
        mode = Mode.PRODUCTION
    )

    val session = bloque.connect("nestor")

    println("=== Creating Virtual Account ===")

    val virtualAccount = session.accounts.virtual.create(
        CreateVirtualAccountParams(
            name = "Main Virtual Account",
            holderUrn = null,    // uses session URN
            webhookUrl = null,
            ledgerId = null,     // null = create new ledger
            metadata = null
        ),
        CreateAccountOptions(waitForCompletion = true, timeout = 60000L)
    )

    println("Virtual Account Created:")
    println("  URN: ${virtualAccount.urn}")
    println("  Ledger ID: ${virtualAccount.ledgerId}")
    println("  Status: ${virtualAccount.status}")

    val sharedLedgerId = virtualAccount.ledgerId!!
    println("Shared Ledger ID: $sharedLedgerId")

    println("\n=== Creating Card Linked to Same Ledger ===")

    val cardAccount = session.accounts.card.create(
        CreateCardAccountParams(
            name = "Credit Card",
            holderUrn = null,         // uses session URN
            webhookUrl = null,
            ledgerId = sharedLedgerId, // use the virtual account's ledger
            metadata = null
        )
    )

    println("Card Account Created:")
    println("  URN: ${cardAccount.urn}")
    println("  Card Number: ${cardAccount.cardNumber}")
    println("  Ledger ID: ${cardAccount.ledgerId}")
    println("  Status: ${cardAccount.status}")

    println("\n=== Verification ===")
    val sameLedger = virtualAccount.ledgerId == cardAccount.ledgerId
    println("Both accounts share the same ledger: $sameLedger")
    println("Shared Ledger ID: $sharedLedgerId")

    println("\n=== Summary ===")
    println("✓ Virtual Account: ${virtualAccount.urn}")
    println("✓ Card Account: ${cardAccount.urn}")
    println("✓ Shared Ledger: $sharedLedgerId")
    println("\nBoth accounts now share the same balance!")
    println("Transfers between them will be instant and free.")
}
