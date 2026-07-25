package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.*
import app.bloque.sdk.core.Mode

/**
 * Kotlin example: Batch transfer funds between multiple accounts
 *
 * This example demonstrates how to execute bulk transfers using the Bloque SDK.
 * Batch transfers enable efficient processing of multiple operations in a single
 * atomic transaction on the blockchain.
 */
fun main() {
    // Initialize the SDK
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "bloque-root",
        originKey = "{your-origin-key-here}",
        mode = Mode.SANDBOX
    )

    // Connect to a user session
    val session = bloque.connect("nestor")


    // ============================================
    // Example 1: Simple batch transfer using direct parameters (simplest)
    // ============================================
    println("=== Example 1: Simple Batch Transfer (Direct Parameters) ===")

    val simpleBatch = session.accounts.batchTransfer(
        BatchTransferParams.builder()
            .reference("batch-simple-001")
            .addOperation(
                fromAccountUrn = "did:bloque:account:card:usr-xxxxx:crd-source123",
                toAccountUrn = "did:bloque:account:card:usr-xxxxx:crd-dest456",
                reference = "transfer-001",
                amount = "1000000",
                asset = SupportedAsset.DUSD_6
            )
            .addOperation(
                fromAccountUrn = "did:bloque:account:card:usr-xxxxx:crd-source123",
                toAccountUrn = "did:bloque:account:card:usr-xxxxx:crd-dest789",
                reference = "transfer-002",
                amount = "2000000",
                asset = SupportedAsset.DUSD_6
            )
            .build()
    )

    println("Total Operations: ${simpleBatch.totalOperations}")
    println("Total Chunks: ${simpleBatch.totalChunks}")
    simpleBatch.chunks.forEach { chunk ->
        println("  Chunk Queue ID: ${chunk.queueId}")
        println("  Status: ${chunk.status}")
        println("  Message: ${chunk.message}")
    }

    // ============================================
    // Example 2: Batch transfer with metadata, webhook, and idempotency key
    // ============================================
    println("\n=== Example 2: Batch Transfer with Metadata ===")

    val batchWithMetadata = session.accounts.batchTransfer(
        BatchTransferParams.builder()
            .reference("batch-payroll-2024-01-15")
            .idempotencyKey("payroll-2024-01-15-unique-key")
            .metadata(mapOf(
                "batch_id" to "batch-2024-01-15",
                "source" to "payroll_system",
                "department" to "engineering"
            ))
            .webhookUrl("https://api.example.com/webhooks/batch-settlement")
            .addOperation(
                fromAccountUrn = "did:bloque:account:virtual:company:vir-treasury",
                toAccountUrn = "did:bloque:account:card:usr-employee1:crd-salary",
                reference = "salary-emp-001",
                amount = "5000000000",
                asset = SupportedAsset.DUSD_6,
                metadata = mapOf(
                    "employee_id" to "emp-001",
                    "payment_type" to "salary",
                    "period" to "2024-01"
                )
            )
            .addOperation(
                fromAccountUrn = "did:bloque:account:virtual:company:vir-treasury",
                toAccountUrn = "did:bloque:account:card:usr-employee2:crd-salary",
                reference = "salary-emp-002",
                amount = "6500000000",
                asset = SupportedAsset.DUSD_6,
                metadata = mapOf(
                    "employee_id" to "emp-002",
                    "payment_type" to "salary",
                    "period" to "2024-01"
                )
            )
            .addOperation(
                fromAccountUrn = "did:bloque:account:virtual:company:vir-treasury",
                toAccountUrn = "did:bloque:account:card:usr-employee3:crd-salary",
                reference = "salary-emp-003",
                amount = "4800000000",
                asset = SupportedAsset.DUSD_6,
                metadata = mapOf(
                    "employee_id" to "emp-003",
                    "payment_type" to "salary",
                    "period" to "2024-01"
                )
            )
            .build()
    )

    println("Payroll Batch Submitted!")
    println("Total Operations: ${batchWithMetadata.totalOperations}")
    println("Total Chunks: ${batchWithMetadata.totalChunks}")
    println("Request ID: ${batchWithMetadata.reqId}")

    // ============================================
    // Example 3: Building operations dynamically
    // ============================================
    println("\n=== Example 3: Dynamic Operation Building ===")

    // Simulate a list of payments to process
    data class Payment(
        val recipientUrn: String,
        val amount: String,
        val reference: String,
        val description: String
    )

    val payments = listOf(
        Payment("did:bloque:account:card:usr-vendor1:crd-001", "150000", "inv-001", "Invoice #001"),
        Payment("did:bloque:account:card:usr-vendor2:crd-002", "275000", "inv-002", "Invoice #002"),
        Payment("did:bloque:account:card:usr-vendor3:crd-003", "89000", "inv-003", "Invoice #003"),
        Payment("did:bloque:account:card:usr-vendor4:crd-004", "432000", "inv-004", "Invoice #004")
    )

    val sourceAccount = "did:bloque:account:virtual:company:vir-payables"

    // Build operations from the payments list
    val dynamicOperations = payments.map { payment ->
        BatchTransferOperation(
            fromAccountUrn = sourceAccount,
            toAccountUrn = payment.recipientUrn,
            reference = payment.reference,
            amount = payment.amount,
            asset = SupportedAsset.DUSD_6,
            metadata = mapOf(
                "description" to payment.description,
                "payment_date" to "2024-01-15"
            )
        )
    }

    val dynamicBatch = session.accounts.batchTransfer(
        BatchTransferParams(
            operations = dynamicOperations,
            reference = "batch-vendor-payments-001",
            metadata = mapOf("batch_type" to "vendor_payments"),
            webhookUrl = "https://api.example.com/webhooks/vendor-payments"
        )
    )

    println("Vendor Payments Batch Submitted!")
    println("Total Operations: ${dynamicBatch.totalOperations}")
    dynamicBatch.chunks.forEachIndexed { index, chunk ->
        println("Chunk $index: ${chunk.queueId} - ${chunk.status}")
    }

    // ============================================
    // Example 4: Using pre-built operations with builder
    // ============================================
    println("\n=== Example 4: Pre-built Operations ===")

    val operation1 = BatchTransferOperation.builder()
        .fromAccountUrn("did:bloque:account:polygon:usr-xxx:pol-001")
        .toAccountUrn("did:bloque:account:polygon:usr-yyy:pol-002")
        .reference("crypto-transfer-001")
        .amount("1000000000000")
        .asset(SupportedAsset.KSM_12)
        .metadata(mapOf("note" to "Crypto payment"))
        .build()

    val operation2 = BatchTransferOperation.builder()
        .fromAccountUrn("did:bloque:account:polygon:usr-xxx:pol-001")
        .toAccountUrn("did:bloque:account:polygon:usr-zzz:pol-003")
        .reference("crypto-transfer-002")
        .amount("2500000000000")
        .asset(SupportedAsset.KSM_12)
        .build()

    val cryptoBatch = session.accounts.batchTransfer(
        BatchTransferParams.builder()
            .reference("batch-crypto-001")
            .addOperation(operation1)
            .addOperation(operation2)
            .build()
    )

    println("Crypto Batch Submitted!")
    println("Total Operations: ${cryptoBatch.totalOperations}")
    println("Chunks: ${cryptoBatch.chunks.size}")

    // ============================================
    // Example 5: Large batch (will be auto-chunked) - using direct parameters
    // ============================================
    println("\n=== Example 5: Large Batch (Auto-Chunking) ===")

    // Create a large batch that will be automatically split into chunks
    val builder = BatchTransferParams.builder()
        .reference("batch-large-distribution-001")
        .webhookUrl("https://api.example.com/webhooks/large-batch")

    // Add 100 operations using direct parameters (no build() needed!)
    repeat(100) { index ->
        builder.addOperation(
            fromAccountUrn = "did:bloque:account:virtual:company:vir-distribution",
            toAccountUrn = "did:bloque:account:card:usr-recipient$index:crd-001",
            reference = "distribution-${String.format("%03d", index)}",
            amount = "100000",
            asset = SupportedAsset.DUSD_6,
            metadata = mapOf("recipient_index" to index)
        )
    }

    val largeBatch = session.accounts.batchTransfer(builder.build())

    println("Large Batch Submitted!")
    println("Total Operations: ${largeBatch.totalOperations}")
    println("Total Chunks: ${largeBatch.totalChunks}")
    println("Chunk Details:")
    largeBatch.chunks.forEachIndexed { index, chunk ->
        println("  Chunk $index:")
        println("    Queue ID: ${chunk.queueId}")
        println("    Status: ${chunk.status}")
        println("    Message: ${chunk.message}")
    }

    // ============================================
    // Example 6: Cross-account type batch transfer - using vararg operations()
    // ============================================
    println("\n=== Example 6: Cross-Account Type Transfers ===")

    // You can also set all operations at once using operations() with vararg
    val crossTypeBatch = session.accounts.batchTransfer(
        BatchTransferParams.builder()
            .reference("batch-cross-type-001")
            .webhookUrl("https://api.example.com/webhooks/cross-type-batch")
            .idempotencyKey("cross-type-batch-001")
            .metadata(mapOf("batch_type" to "cross-type-batch"))
            .operations(
                // Card to Virtual
                BatchTransferOperation(
                    fromAccountUrn = "did:bloque:account:card:usr-xxx:crd-001",
                    toAccountUrn = "did:bloque:account:virtual:usr-xxx:vir-001",
                    reference = "card-to-virtual-001",
                    amount = "500000",
                    asset = SupportedAsset.DUSD_6
                ),
                // Virtual to Bancolombia
                BatchTransferOperation(
                    fromAccountUrn = "did:bloque:account:virtual:usr-xxx:vir-001",
                    toAccountUrn = "did:bloque:account:bancolombia:usr-xxx:ban-001",
                    reference = "virtual-to-bank-001",
                    amount = "300000",
                    asset = SupportedAsset.COP_2
                ),
                // Polygon to Polygon (different users)
                BatchTransferOperation(
                    fromAccountUrn = "did:bloque:account:polygon:usr-xxx:pol-001",
                    toAccountUrn = "did:bloque:account:polygon:usr-yyy:pol-002",
                    reference = "polygon-to-polygon-001",
                    amount = "1000000000000",
                    asset = SupportedAsset.KSM_12
                )
            )
            .build()
    )

    println("Cross-Type Batch Submitted!")
    println("Total Operations: ${crossTypeBatch.totalOperations}")
    crossTypeBatch.chunks.forEach { chunk ->
        println("Queue ID: ${chunk.queueId} - Status: ${chunk.status}")
    }

    println("\n=== All Examples Completed ===")
}
