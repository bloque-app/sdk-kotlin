package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.*
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile

/**
 * Kotlin example: Bancolombia account operations
 *
 * This example demonstrates how to create and manage Bancolombia accounts
 * using the Bloque SDK in idiomatic Kotlin.
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
    // Example 1: Create a simple Bancolombia account
    // ============================================
    println("=== Example 1: Create Simple Bancolombia Account ===")

    val simpleAccount = session.accounts.bancolombia.create()

    println("Account URN: ${simpleAccount.urn}")
    println("Account ID: ${simpleAccount.id}")
    println("Reference Code: ${simpleAccount.referenceCode}")
    println("Details ID: ${simpleAccount.detailsId}")
    println("Payment Agreement Code: ${simpleAccount.paymentAgreementCode}")
    println("Bank Account Number: ${simpleAccount.bankAccountNumber}")
    println("Bank Account Type: ${simpleAccount.bankAccountType}")
    println("Bank Account Holder: ${simpleAccount.bankAccountHolderName}")
    println("Holder ID: ${simpleAccount.bankAccountHolderIdType} ${simpleAccount.bankAccountHolderIdValue}")
    println("Networks: ${simpleAccount.network}")
    println("Status: ${simpleAccount.status}")
    println("Owner: ${simpleAccount.ownerUrn}")

    // ============================================
    // Example 2: Create account with custom name
    // ============================================
    println("\n=== Example 2: Create Account with Name ===")

    val namedAccount = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "My Savings Account"
        )
    )

    println("Account URN: ${namedAccount.urn}")
    println("Account ID: ${namedAccount.id}")
    println("Status: ${namedAccount.status}")
    println("Metadata: ${namedAccount.metadata}")

    // ============================================
    // Example 3: Create account with webhook
    // ============================================
    println("\n=== Example 3: Create Account with Webhook ===")

    val accountWithWebhook = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Monitored Account",
            webhookUrl = "https://myapp.com/webhooks/bancolombia"
        )
    )

    println("Account URN: ${accountWithWebhook.urn}")
    println("Webhook URL: ${accountWithWebhook.webhookUrl}")

    // ============================================
    // Example 4: Create account and wait for active status
    // ============================================
    println("\n=== Example 4: Create and Wait for Active Status ===")

    val activeAccount = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Active Account"
        ),
        CreateAccountOptions(
            waitLedger = true,
            timeout = 60000  // Wait up to 60 seconds
        )
    )

    println("Account URN: ${activeAccount.urn}")
    println("Status: ${activeAccount.status}")  // Should be "active"
    println("Ledger ID: ${activeAccount.ledgerId}")

    // ============================================
    // Example 5: Create account with idempotency key
    // ============================================
    println("\n=== Example 5: Create with Idempotency Key ===")

    val idempotentAccount = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Idempotent Account",
            idempotencyKey = "unique-request-id-12345"
        )
    )

    println("Account URN: ${idempotentAccount.urn}")

    // Calling again with same idempotency key returns the same account
    val sameAccount = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Different Name",  // This will be ignored
            idempotencyKey = "unique-request-id-12345"
        )
    )

    println("Same account? ${idempotentAccount.urn == sameAccount.urn}")  // true

    // ============================================
    // Example 6: Create account with custom metadata
    // ============================================
    println("\n=== Example 6: Create with Custom Metadata ===")

    val accountWithMetadata = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Business Account",
            metadata = mapOf(
                "department" to "Finance",
                "costCenter" to "CC-001",
                "approvedBy" to "admin@company.com"
            )
        )
    )

    println("Account URN: ${accountWithMetadata.urn}")
    println("Metadata: ${accountWithMetadata.metadata}")

    // ============================================
    // Example 7: List all Bancolombia accounts
    // ============================================
    println("\n=== Example 7: List All Accounts ===")

    val allAccounts = session.accounts.bancolombia.list()

    println("Total accounts: ${allAccounts.size}")
    allAccounts.forEach { account ->
        println("  - ${account.urn} (${account.status})")
    }

    // ============================================
    // Example 8: List active accounts only
    // ============================================
    println("\n=== Example 8: List Active Accounts ===")

    val activeAccounts = session.accounts.bancolombia.list(
        ListBancolombiaParams(status = "active")
    )

    println("Active accounts: ${activeAccounts.size}")

    // ============================================
    // Example 9: Get account by URN
    // ============================================
    println("\n=== Example 9: Get Account by URN ===")

    val fetchedAccount = session.accounts.bancolombia.get(simpleAccount.urn)

    println("Account URN: ${fetchedAccount.urn}")
    println("Reference Code: ${fetchedAccount.referenceCode}")
    println("Status: ${fetchedAccount.status}")
    println("Created At: ${fetchedAccount.createdAt}")

    // ============================================
    // Example 10: Update account name
    // ============================================
    println("\n=== Example 10: Update Account Name ===")

    val renamedAccount = session.accounts.bancolombia.updateName(
        urn = simpleAccount.urn,
        name = "My Primary Account"
    )

    println("Updated name in metadata: ${renamedAccount.metadata}")

    // ============================================
    // Example 11: Update account metadata
    // ============================================
    println("\n=== Example 11: Update Account Metadata ===")

    val updatedAccount = session.accounts.bancolombia.updateMetadata(
        UpdateBancolombiaMetadataParams(
            urn = simpleAccount.urn,
            metadata = mapOf(
                "name" to "Updated Account",
                "tags" to listOf("primary", "business"),
                "settings" to mapOf(
                    "notifications" to true,
                    "autoReconcile" to false
                )
            )
        )
    )

    println("Updated metadata: ${updatedAccount.metadata}")

    // ============================================
    // Example 12: Account lifecycle management
    // ============================================
    println("\n=== Example 12: Account Lifecycle ===")

    // Freeze account (temporarily disable)
    val frozenAccount = session.accounts.bancolombia.freeze(simpleAccount.urn)
    println("After freeze - Status: ${frozenAccount.status}")  // "frozen"

    // Reactivate account
    val reactivatedAccount = session.accounts.bancolombia.activate(simpleAccount.urn)
    println("After activate - Status: ${reactivatedAccount.status}")  // "active"

    // Disable account (permanent)
    val disabledAccount = session.accounts.bancolombia.disable(simpleAccount.urn)
    println("After disable - Status: ${disabledAccount.status}")  // "disabled"

    // ============================================
    // Example 13: Delete account
    // ============================================
    println("\n=== Example 13: Delete Account ===")

    val deletedAccount = session.accounts.bancolombia.delete(namedAccount.urn)
    println("After delete - Status: ${deletedAccount.status}")  // "deleted"

    println("\n=== All Examples Completed ===")
}
