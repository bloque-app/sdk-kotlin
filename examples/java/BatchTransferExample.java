package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.*;
import app.bloque.sdk.core.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java example: Batch transfer funds between multiple accounts
 *
 * This example demonstrates how to execute bulk transfers using the Bloque SDK in Java.
 * Batch transfers enable efficient processing of multiple operations in a single
 * atomic transaction on the blockchain.
 */
public class BatchTransferExample {

    public static void main(String[] args) {
        // Initialize the SDK using builder pattern
        BloqueSDK bloque = BloqueSDK.builder()
                .origin("my-app-origin")
                .apiKey("sk_test_your_api_key_here")
                .mode(Mode.SANDBOX)
                .build();

        // Connect to a user session
        UserSession session = bloque.connect("nestor");

        // ============================================
        // Example 1: Simple batch transfer using direct parameters (simplest)
        // ============================================
        System.out.println("=== Example 1: Simple Batch Transfer (Direct Parameters) ===");

        BatchTransferResult simpleBatch = session.getAccounts().batchTransfer(
                BatchTransferParams.builder()
                        .reference("batch-simple-001")
                        // Direct parameters - no need to build each operation!
                        .addOperation(
                                "did:bloque:account:card:usr-xxxxx:crd-source123",  // from
                                "did:bloque:account:card:usr-xxxxx:crd-dest456",    // to
                                "transfer-001",                                      // reference
                                "1000000",                                           // amount
                                SupportedAsset.DUSD_6,                               // asset
                                null                                                 // metadata (optional)
                        )
                        .addOperation(
                                "did:bloque:account:card:usr-xxxxx:crd-source123",
                                "did:bloque:account:card:usr-xxxxx:crd-dest789",
                                "transfer-002",
                                "2000000",
                                SupportedAsset.DUSD_6,
                                null
                        )
                        .build()
        );

        System.out.println("Total Operations: " + simpleBatch.getTotalOperations());
        System.out.println("Total Chunks: " + simpleBatch.getTotalChunks());
        for (BatchTransferChunkResult chunk : simpleBatch.getChunks()) {
            System.out.println("  Chunk Queue ID: " + chunk.getQueueId());
            System.out.println("  Status: " + chunk.getStatus());
            System.out.println("  Message: " + chunk.getMessage());
        }

        // ============================================
        // Example 2: Batch transfer with metadata and webhook
        // ============================================
        System.out.println("\n=== Example 2: Batch Transfer with Metadata ===");

        Map<String, Object> batchMetadata = new HashMap<>();
        batchMetadata.put("batch_id", "batch-2024-01-15");
        batchMetadata.put("source", "payroll_system");
        batchMetadata.put("department", "engineering");

        Map<String, Object> emp1Metadata = new HashMap<>();
        emp1Metadata.put("employee_id", "emp-001");
        emp1Metadata.put("payment_type", "salary");
        emp1Metadata.put("period", "2024-01");

        Map<String, Object> emp2Metadata = new HashMap<>();
        emp2Metadata.put("employee_id", "emp-002");
        emp2Metadata.put("payment_type", "salary");
        emp2Metadata.put("period", "2024-01");

        BatchTransferResult payrollBatch = session.getAccounts().batchTransfer(
                BatchTransferParams.builder()
                        .reference("batch-payroll-2024-01-15")
                        .idempotencyKey("payroll-2024-01-15-unique-key")
                        .metadata(batchMetadata)
                        .webhookUrl("https://api.example.com/webhooks/batch-settlement")
                        // Direct parameters with metadata
                        .addOperation(
                                "did:bloque:account:virtual:company:vir-treasury",
                                "did:bloque:account:card:usr-employee1:crd-salary",
                                "salary-emp-001",
                                "5000000000",
                                SupportedAsset.DUSD_6,
                                emp1Metadata
                        )
                        .addOperation(
                                "did:bloque:account:virtual:company:vir-treasury",
                                "did:bloque:account:card:usr-employee2:crd-salary",
                                "salary-emp-002",
                                "6500000000",
                                SupportedAsset.DUSD_6,
                                emp2Metadata
                        )
                        .build()
        );

        System.out.println("Payroll Batch Submitted!");
        System.out.println("Total Operations: " + payrollBatch.getTotalOperations());
        System.out.println("Total Chunks: " + payrollBatch.getTotalChunks());
        System.out.println("Request ID: " + payrollBatch.getReqId());

        // ============================================
        // Example 3: Building operations dynamically
        // ============================================
        System.out.println("\n=== Example 3: Dynamic Operation Building ===");

        // Simulate a list of payments to process
        List<BatchTransferOperation> dynamicOperations = new ArrayList<>();
        String sourceAccount = "did:bloque:account:virtual:company:vir-payables";

        String[][] payments = {
                {"did:bloque:account:card:usr-vendor1:crd-001", "150000", "inv-001", "Invoice #001"},
                {"did:bloque:account:card:usr-vendor2:crd-002", "275000", "inv-002", "Invoice #002"},
                {"did:bloque:account:card:usr-vendor3:crd-003", "89000", "inv-003", "Invoice #003"},
                {"did:bloque:account:card:usr-vendor4:crd-004", "432000", "inv-004", "Invoice #004"}
        };

        for (String[] payment : payments) {
            Map<String, Object> paymentMetadata = new HashMap<>();
            paymentMetadata.put("description", payment[3]);
            paymentMetadata.put("payment_date", "2024-01-15");

            // Create operation directly without builder
            dynamicOperations.add(new BatchTransferOperation(
                    sourceAccount,      // from
                    payment[0],         // to
                    payment[2],         // reference
                    payment[1],         // amount
                    SupportedAsset.DUSD_6,
                    paymentMetadata
            ));
        }

        BatchTransferResult vendorBatch = session.getAccounts().batchTransfer(
                BatchTransferParams.builder()
                        .reference("batch-vendor-payments-001")
                        .addOperations(dynamicOperations)
                        .webhookUrl("https://api.example.com/webhooks/vendor-payments")
                        .build()
        );

        System.out.println("Vendor Payments Batch Submitted!");
        System.out.println("Total Operations: " + vendorBatch.getTotalOperations());
        for (int i = 0; i < vendorBatch.getChunks().size(); i++) {
            BatchTransferChunkResult chunk = vendorBatch.getChunks().get(i);
            System.out.println("Chunk " + i + ": " + chunk.getQueueId() + " - " + chunk.getStatus());
        }

        // ============================================
        // Example 4: Cross-account type batch transfer (using direct parameters)
        // ============================================
        System.out.println("\n=== Example 4: Cross-Account Type Transfers ===");

        BatchTransferResult crossTypeBatch = session.getAccounts().batchTransfer(
                BatchTransferParams.builder()
                        .reference("batch-cross-type-001")
                        // Card to Virtual - direct parameters
                        .addOperation(
                                "did:bloque:account:card:usr-xxx:crd-001",
                                "did:bloque:account:virtual:usr-xxx:vir-001",
                                "card-to-virtual-001",
                                "500000",
                                SupportedAsset.DUSD_6,
                                null
                        )
                        // Virtual to Bancolombia
                        .addOperation(
                                "did:bloque:account:virtual:usr-xxx:vir-001",
                                "did:bloque:account:bancolombia:usr-xxx:ban-001",
                                "virtual-to-bank-001",
                                "300000",
                                SupportedAsset.COP_2,
                                null
                        )
                        // Polygon to Polygon
                        .addOperation(
                                "did:bloque:account:polygon:usr-xxx:pol-001",
                                "did:bloque:account:polygon:usr-yyy:pol-002",
                                "polygon-to-polygon-001",
                                "1000000000000",
                                SupportedAsset.KSM_12,
                                null
                        )
                        .build()
        );

        System.out.println("Cross-Type Batch Submitted!");
        System.out.println("Total Operations: " + crossTypeBatch.getTotalOperations());
        for (BatchTransferChunkResult chunk : crossTypeBatch.getChunks()) {
            System.out.println("Queue ID: " + chunk.getQueueId() + " - Status: " + chunk.getStatus());
        }

        System.out.println("\n=== All Examples Completed ===");
    }
}
