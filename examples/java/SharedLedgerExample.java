package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.CardAccount;
import app.bloque.sdk.accounts.CreateCardAccountParams;
import app.bloque.sdk.accounts.CreateVirtualAccountParams;
import app.bloque.sdk.accounts.CreateAccountOptions;
import app.bloque.sdk.accounts.VirtualAccount;
import app.bloque.sdk.core.Mode;

/**
 * Example: Creating multiple accounts linked to the same ledger
 *
 * This demonstrates how to:
 * 1. Create a virtual account (which gets assigned a ledger)
 * 2. Create a card account linked to the same ledger
 *
 * Both accounts will share the same balance through the shared ledger.
 */
public class SharedLedgerExample {

    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.PRODUCTION)
            .build();

        UserSession session = bloque.connect("nestor");

        System.out.println("=== Creating Virtual Account ===");

        VirtualAccount virtualAccount = session.getAccounts()
            .getVirtual()
            .create(
                new CreateVirtualAccountParams(
                    "Main Virtual Account",  // name
                    null,                     // holderUrn (uses session URN)
                    null,                     // webhookUrl
                    null,                     // ledgerId (null = create new ledger)
                    null                      // metadata
                ),
                new CreateAccountOptions(true, 60000L)
            );

        System.out.println("Virtual Account Created:");
        System.out.println("  URN: " + virtualAccount.getUrn());
        System.out.println("  Ledger ID: " + virtualAccount.getLedgerId());
        System.out.println("  Status: " + virtualAccount.getStatus());

        String sharedLedgerId = virtualAccount.getLedgerId();
        System.out.println("Shared Ledger ID: " + sharedLedgerId);

        System.out.println("\n=== Creating Card Linked to Same Ledger ===");

        CardAccount cardAccount = session.getAccounts()
            .getCard()
            .create(new CreateCardAccountParams(
                "Credit Card",        // name
                null,                 // holderUrn (uses session URN)
                null,                 // webhookUrl
                sharedLedgerId,       // ledgerId (use the virtual account's ledger)
                null                  // metadata
            ));

        System.out.println("Card Account Created:");
        System.out.println("  URN: " + cardAccount.getUrn());
        System.out.println("  Ledger ID: " + cardAccount.getLedgerId());
        System.out.println("  Status: " + cardAccount.getStatus());

        System.out.println("\n=== Verification ===");
        boolean sameLedger = virtualAccount.getLedgerId().equals(cardAccount.getLedgerId());
        System.out.println("Both accounts share the same ledger: " + sameLedger);
        System.out.println("Shared Ledger ID: " + sharedLedgerId);

        System.out.println("\n=== Summary ===");
        System.out.println("✓ Virtual Account: " + virtualAccount.getUrn());
        System.out.println("✓ Card Account: " + cardAccount.getUrn());
        System.out.println("✓ Shared Ledger: " + sharedLedgerId);
        System.out.println("\nBoth accounts now share the same balance!");
        System.out.println("Transfers between them will be instant and free.");
    }
}
