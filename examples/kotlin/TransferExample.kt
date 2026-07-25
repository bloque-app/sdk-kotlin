package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.*
import app.bloque.sdk.core.Mode

/**
 * Kotlin example: Transfer money between accounts
 *
 * This example demonstrates how to transfer funds between different account types
 * using the Bloque SDK in idiomatic Kotlin.
 */
fun main() {
    // Initialize the SDK
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "my-app-origin",
        originKey = "sk_test_your_origin_key_here",
        mode = Mode.SANDBOX
    )

    // Connect to a user session
    val session = bloque.connect("nestor")

    // ============================================
    // Example 1: Simple transfer between card accounts
    // ============================================
    println("=== Example 1: Simple Transfer ===")

    val simpleTransfer = session.accounts.transfer(
        TransferParams(
            sourceUrn = "did:bloque:account:card:usr-xxxxx:crd-source123",
            destinationUrn = "did:bloque:account:card:usr-xxxxx:crd-dest456",
            amount = "1000000",  // amount in smallest unit
            asset = SupportedAsset.DUSD_6
        )
    )

    println("Queue ID: ${simpleTransfer.queueId}")
    println("Status: ${simpleTransfer.status}")
    println("Message: ${simpleTransfer.message}")

    // ============================================
    // Example 2: Transfer with metadata
    // ============================================
    println("\n=== Example 2: Transfer with Metadata ===")

    val transferWithMetadata = session.accounts.transfer(
        TransferParams(
            sourceUrn = "did:bloque:account:card:usr-xxxxx:crd-source123",
            destinationUrn = "did:bloque:account:card:usr-xxxxx:crd-dest456",
            amount = "500000",
            asset = SupportedAsset.DUSD_6,
            metadata = mapOf(
                "reference" to "INV-2026-001",
                "description" to "Payment for services",
                "category" to "business"
            )
        )
    )

    println("Queue ID: ${transferWithMetadata.queueId}")
    println("Status: ${transferWithMetadata.status}")

    // ============================================
    // Example 3: Transfer using KSM asset
    // ============================================
    println("\n=== Example 3: Transfer with KSM Asset ===")

    val ksmTransfer = session.accounts.transfer(
        TransferParams(
            sourceUrn = "did:bloque:account:polygon:usr-xxxxx:pol-source789",
            destinationUrn = "did:bloque:account:polygon:usr-xxxxx:pol-dest012",
            amount = "1000000000000",  // amount in smallest unit (12 decimals for KSM)
            asset = SupportedAsset.KSM_12
        )
    )

    println("Queue ID: ${ksmTransfer.queueId}")
    println("Status: ${ksmTransfer.status}")

    // ============================================
    // Example 4: Transfer between different account types
    // ============================================
    println("\n=== Example 4: Cross-Account Type Transfer ===")

    // First, get or create accounts
    val cardAccount = session.accounts.card.create(
        CreateCardAccountParams(name = "Source Card")
    )

    val virtualAccount = session.accounts.virtual.create(
        CreateVirtualAccountParams(name = "Destination Virtual")
    )

    // Transfer from card to virtual account
    val crossAccountTransfer = session.accounts.transfer(
        TransferParams(
            sourceUrn = cardAccount.urn,
            destinationUrn = virtualAccount.urn,
            amount = "250000",
            asset = SupportedAsset.DUSD_6
        )
    )

    println("Transferred from Card to Virtual Account")
    println("Queue ID: ${crossAccountTransfer.queueId}")
    println("Status: ${crossAccountTransfer.status}")

    // ============================================
    // Example 5: Check balance after transfer
    // ============================================
    println("\n=== Example 5: Check Balance After Transfer ===")

    val balances = session.accounts.balanceByAccount(
        GetAccountBalanceParams(urn = cardAccount.urn)
    )

    balances.forEach { (asset, balance) ->
        println("Asset: $asset")
        println("  Current Balance: ${balance.current}")
        println("  Pending: ${balance.pending}")
        println("  In: ${balance.`in`}")
        println("  Out: ${balance.out}")
    }

    // ============================================
    // Example 6: Get account movements
    // ============================================
    println("\n=== Example 6: Get Account Movements ===")

    // Get movements for any account using the generic movements method
    val movements = session.accounts.movements(
        ListMovementsParams(
            urn = cardAccount.urn,
            asset = SupportedAsset.DUSD_6.value,
            limit = 10
        )
    )

    println("Found ${movements.data.size} movements:")
    movements.data.forEach { movement ->
        println("  - Reference: ${movement.reference}")
        println("    Amount: ${movement.amount} ${movement.asset}")
        println("    Direction: ${movement.direction}")
        println("    Date: ${movement.createdAt}")
    }

    // ============================================
    // Example 7: Functional approach with result handling
    // ============================================
    println("\n=== Example 7: Functional Approach ===")

    // Using Kotlin's scope functions for cleaner code
    session.accounts.transfer(
        TransferParams(
            sourceUrn = cardAccount.urn,
            destinationUrn = virtualAccount.urn,
            amount = "100000",
            asset = SupportedAsset.DUSD_6,
            metadata = mapOf("type" to "recurring")
        )
    ).also { result ->
        println("Transfer completed!")
        println("  Queue ID: ${result.queueId}")
        println("  Status: ${result.status}")
        println("  Message: ${result.message}")
    }
}
