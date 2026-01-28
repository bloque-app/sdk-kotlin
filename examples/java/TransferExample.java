package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.*;
import app.bloque.sdk.core.Mode;

import java.util.HashMap;
import java.util.Map;

/**
 * Java example: Transfer money between accounts
 *
 * This example demonstrates how to transfer funds between different account types
 * using the Bloque SDK in Java.
 */
public class TransferExample {

    public static void main(String[] args) {
        // Initialize the SDK
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.SANDBOX)
            .build();

        // Connect to a user session
        UserSession session = bloque.connect("nestor");

        // ============================================
        // Example 1: Simple transfer between card accounts
        // ============================================
        System.out.println("=== Example 1: Simple Transfer ===");

        TransferParams simpleTransfer = new TransferParams(
            "did:bloque:account:card:usr-xxxxx:crd-source123",  // sourceUrn
            "did:bloque:account:card:usr-xxxxx:crd-dest456",    // destinationUrn
            "1000000",                                           // amount (in smallest unit)
            SupportedAsset.DUSD_6                                // asset
        );

        TransferResult result = session.getAccounts().transfer(simpleTransfer);
        System.out.println("Queue ID: " + result.getQueueId());
        System.out.println("Status: " + result.getStatus());
        System.out.println("Message: " + result.getMessage());

        // ============================================
        // Example 2: Transfer with metadata
        // ============================================
        System.out.println("\n=== Example 2: Transfer with Metadata ===");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", "INV-2026-001");
        metadata.put("description", "Payment for services");
        metadata.put("category", "business");

        TransferParams transferWithMetadata = new TransferParams(
            "did:bloque:account:card:usr-xxxxx:crd-source123",
            "did:bloque:account:card:usr-xxxxx:crd-dest456",
            "500000",
            SupportedAsset.DUSD_6,
            metadata
        );

        TransferResult resultWithMetadata = session.getAccounts().transfer(transferWithMetadata);
        System.out.println("Queue ID: " + resultWithMetadata.getQueueId());
        System.out.println("Status: " + resultWithMetadata.getStatus());

        // ============================================
        // Example 3: Transfer using KSM asset
        // ============================================
        System.out.println("\n=== Example 3: Transfer with KSM Asset ===");

        TransferParams ksmTransfer = new TransferParams(
            "did:bloque:account:polygon:usr-xxxxx:pol-source789",
            "did:bloque:account:polygon:usr-xxxxx:pol-dest012",
            "1000000000000",  // amount in smallest unit (12 decimals for KSM)
            SupportedAsset.KSM_12
        );

        TransferResult ksmResult = session.getAccounts().transfer(ksmTransfer);
        System.out.println("Queue ID: " + ksmResult.getQueueId());
        System.out.println("Status: " + ksmResult.getStatus());

        // ============================================
        // Example 4: Transfer between different account types
        // ============================================
        System.out.println("\n=== Example 4: Cross-Account Type Transfer ===");

        // First, get or create accounts
        CardAccount cardAccount = session.getAccounts().getCard().create(
            new CreateCardAccountParams("Source Card")
        );

        VirtualAccount virtualAccount = session.getAccounts().getVirtual().create(
            new CreateVirtualAccountParams("Destination Virtual")
        );

        // Transfer from card to virtual account
        TransferParams crossAccountTransfer = new TransferParams(
            cardAccount.getUrn(),
            virtualAccount.getUrn(),
            "250000",
            SupportedAsset.DUSD_6
        );

        TransferResult crossResult = session.getAccounts().transfer(crossAccountTransfer);
        System.out.println("Transferred from Card to Virtual Account");
        System.out.println("Queue ID: " + crossResult.getQueueId());
        System.out.println("Status: " + crossResult.getStatus());

        // ============================================
        // Example 5: Check balance after transfer
        // ============================================
        System.out.println("\n=== Example 5: Check Balance After Transfer ===");

        Map<String, TokenBalance> balances = session.getAccounts()
            .balanceByAccount(new GetAccountBalanceParams(cardAccount.getUrn()));

        balances.forEach((asset, balance) -> {
            System.out.println("Asset: " + asset);
            System.out.println("  Current Balance: " + balance.getCurrent());
            System.out.println("  Pending: " + balance.getPending());
            System.out.println("  In: " + balance.getIn());
            System.out.println("  Out: " + balance.getOut());
        });
    }
}
