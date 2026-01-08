package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.*;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.identity.*;

import java.util.List;

/**
 * Java example: Register a new user
 *
 * This example demonstrates how to register individual and business users
 * using the Bloque SDK in Java.
 */
public class RegisterExample {

    public static void main(String[] args) {
        // Initialize the SDK
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("bloque-root")
            .apiKey("sk_live_your_api_key_here")
            .mode(Mode.PRODUCTION)
            .build();

        // ============================================
        // Example 1: Register an individual user
        // ============================================
        System.out.println("=== Example 1: Register Individual User ===");

        UserProfile profile = new UserProfile(
            "John",                    // firstName
            "Doe",                     // lastName
            "1990-01-01",             // birthdate
            "john.doe@example.com",   // email
            "+1234567890",            // phone
            "USA",                    // nationality
            "USA",                    // countryOfResidence
            "123 Main Street",        // addressLine1
            "Apt 4B",                 // addressLine2
            "New York",               // city
            "NY",                     // state
            "10001",                  // postalCode
            "USA",                    // country
            "SSN",                    // documentType
            "123-45-6789",            // documentNumber
            null,                     // documentIssueDate
            null                      // documentExpiryDate
        );

        UserSession session = bloque.register(
            "johndoe123",
            new IndividualRegisterParams(profile)
        );

        System.out.println("User registered successfully!");
        System.out.println("User URN: " + session.getUserUrn());

        // ============================================
        // Example 2: List user's virtual accounts
        // ============================================
        System.out.println("\n=== Example 2: List Virtual Accounts ===");

        List<VirtualAccount> virtualAccounts = session.getAccounts()
            .getVirtual()
            .list();

        System.out.println("Total virtual accounts: " + virtualAccounts.size());

        for (VirtualAccount account : virtualAccounts) {
            System.out.println("  - URN: " + account.getUrn());
            System.out.println("    Status: " + account.getStatus());
        }

        // ============================================
        // Example 3: Create accounts for the new user
        // ============================================
        System.out.println("\n=== Example 3: Create Accounts ===");

        // Create a card account
        CardAccount cardAccount = session.getAccounts()
            .getCard()
            .create(new CreateCardAccountParams("My First Card"));

        System.out.println("Card Account created: " + cardAccount.getUrn());

        // Create a virtual account
        VirtualAccount virtualAccount = session.getAccounts()
            .getVirtual()
            .create(new CreateVirtualAccountParams("My Savings"));

        System.out.println("Virtual Account created: " + virtualAccount.getUrn());

        // ============================================
        // Example 4: Register a business user
        // ============================================
        System.out.println("\n=== Example 4: Register Business User ===");

        BusinessProfile businessProfile = new BusinessProfile(
            "Acme Corporation",        // businessName
            "Technology",              // industry
            "contact@acme.com",        // email
            "+1987654321",             // phone
            "USA",                     // countryOfIncorporation
            "2015-06-15",              // incorporationDate
            "456 Business Ave",        // addressLine1
            "Suite 100",               // addressLine2
            "San Francisco",           // city
            "CA",                      // state
            "94102",                   // postalCode
            "USA",                     // country
            "EIN",                     // documentType
            "12-3456789",              // documentNumber
            null,                      // documentIssueDate
            null                       // documentExpiryDate
        );

        UserSession businessSession = bloque.register(
            "acme-corp",
            new BusinessRegisterParams(businessProfile)
        );

        System.out.println("Business registered successfully!");
        System.out.println("Business URN: " + businessSession.getUserUrn());
    }
}
