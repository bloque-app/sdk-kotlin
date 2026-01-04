package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.*;
import app.bloque.sdk.compliance.*;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.identity.*;
import app.bloque.sdk.orgs.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Java usage examples for Bloque SDK
 *
 * This demonstrates that the SDK is fully compatible with Java
 */
public class JavaExample {

    public static void main(String[] args) {
        // Example 1: Creating the SDK using Builder pattern (recommended for Java)
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.SANDBOX)
            .build();

        // Alternative: Direct creation
        BloqueSDK bloqueAlt = BloqueSDK.create(
            "my-app-origin",
            "sk_test_your_api_key_here",
            Mode.SANDBOX
        );

        // Example 2: Connect to existing user (using alias: username, email, or phone)
        UserSession session = bloque.connect("nestor");

        // Example 3: Create a Bancolombia account
        CreateBancolombiaAccountParams bancolombiaParams = new CreateBancolombiaAccountParams(
            "Cuenta de Ahorros",  // name
            null,                 // holderUrn (uses session URN)
            null,                 // webhookUrl
            null,                 // ledgerId
            new HashMap<>()       // metadata
        );

        BancolombiaAccount bancolombiaAccount = session.getAccounts()
            .getBancolombia()
            .create(bancolombiaParams);

        System.out.println("Bancolombia Account URN: " + bancolombiaAccount.getUrn());
        System.out.println("Reference Code: " + bancolombiaAccount.getReferenceCode());

        // Example 4: Create a Card account
        CreateCardAccountParams cardParams = new CreateCardAccountParams(
            "My Credit Card",
            null,
            null,
            null,
            new HashMap<>()
        );

        CardAccount cardAccount = session.getAccounts().getCard().create(cardParams);
        System.out.println("Card Account URN: " + cardAccount.getUrn());

        // Example 5: Transfer between accounts
        TransferParams transferParams = new TransferParams(
            bancolombiaAccount.getUrn(),  // source
            cardAccount.getUrn(),          // destination
            "100.00",                      // amount
            SupportedAsset.DUSD_6,         // asset
            new HashMap<>()                // metadata
        );

        TransferResult transfer = session.getAccounts().transfer(transferParams);
        System.out.println("Transfer Queue ID: " + transfer.getQueueId());
        System.out.println("Transfer Status: " + transfer.getStatus());

        // Example 6: List card accounts
        ListCardParams listParams = new ListCardParams(null, "active");
        var cards = session.getAccounts().getCard().list(listParams);
        System.out.println("Found " + cards.size() + " card accounts");

        // Example 7: Get account balance
        GetBalanceParams balanceParams = new GetBalanceParams(
            cardAccount.getUrn(),
            SupportedAsset.DUSD_6
        );

        Map<String, TokenBalance> balances = session.getAccounts()
            .getCard()
            .balance(balanceParams);

        balances.forEach((asset, balance) -> {
            System.out.println("Asset: " + asset);
            System.out.println("  Current: " + balance.getCurrent());
            System.out.println("  Pending: " + balance.getPending());
        });

        // Example 8: Register new individual user
        UserProfile userProfile = new UserProfile(
            "John",                  // firstName
            "Doe",                   // lastName
            "1990-01-01",           // birthdate
            "john@example.com",     // email
            "+1234567890",          // phone
            "US",                   // nationality
            "US",                   // countryOfResidence
            "123 Main St",          // addressLine1
            "Apt 4B",               // addressLine2
            "New York",             // city
            "NY",                   // state
            "10001",                // postalCode
            "US",                   // country
            "passport",             // documentType
            "AB123456",             // documentNumber
            "2020-01-01",           // documentIssueDate
            "2030-01-01"            // documentExpiryDate
        );

        IndividualRegisterParams registerParams = new IndividualRegisterParams(
            userProfile,
            "+1234567890",  // alias
            "my-app-origin",      // origin
            new HashMap<>()       // metadata
        );

        // Note: register() would be called on a fresh SDK instance
        // UserSession newUserSession = bloque.register("+1234567890", registerParams);

        // Example 9: Start KYC verification
        KycVerificationParams kycParams = new KycVerificationParams(
            session.getUrn(),
            "https://myapp.com/webhooks/kyc"
        );

        KycVerificationResponse kycResponse = session.getCompliance()
            .getKyc()
            .startVerification(kycParams);

        System.out.println("KYC Verification URL: " + kycResponse.getUrl());
        System.out.println("KYC Status: " + kycResponse.getStatus());

        // Example 10: Create an organization
        OrgProfile orgProfile = new OrgProfile(
            "Acme Corporation",        // legalName
            "123456789",               // taxId
            "2020-01-01",              // incorporationDate
            "LLC",                     // businessType
            "US",                      // incorporationCountryCode
            "456 Business Ave",        // addressLine1
            "Suite 100",               // addressLine2
            "San Francisco",           // city
            "CA",                      // state
            "94102",                   // postalCode
            "US",                      // country
            "https://acme.com",        // website
            "contact@acme.com",        // email
            "+14155551234",            // phone
            "Technology"               // industry
        );

        CreateOrgParams orgParams = new CreateOrgParams(orgProfile, new HashMap<>());
        Organization org = session.getOrgs().create(orgParams);

        System.out.println("Organization URN: " + org.getUrn());
        System.out.println("Organization Status: " + org.getStatus());

        // Example 11: Update account status
        BancolombiaAccount frozenAccount = session.getAccounts()
            .getBancolombia()
            .freeze(bancolombiaAccount.getUrn());

        System.out.println("Account frozen, status: " + frozenAccount.getStatus());

        // Reactivate
        BancolombiaAccount activeAccount = session.getAccounts()
            .getBancolombia()
            .activate(frozenAccount.getUrn());

        System.out.println("Account reactivated, status: " + activeAccount.getStatus());

        // Example 12: Working with Polygon wallets
        CreatePolygonAccountParams polygonParams = new CreatePolygonAccountParams(
            "My Polygon Wallet",
            null,
            null,
            null,
            new HashMap<>()
        );

        PolygonAccount polygonAccount = session.getAccounts()
            .getPolygon()
            .create(polygonParams);

        System.out.println("Polygon Wallet Address: " + polygonAccount.getWalletAddress());

        // Example 13: Error handling
        try {
            // This would throw an error if account doesn't exist
            session.getAccounts().getBancolombia().activate("invalid-urn");
        } catch (app.bloque.sdk.core.BloqueNotFoundError e) {
            System.err.println("Account not found: " + e.getMessage());
        } catch (app.bloque.sdk.core.BloqueAPIError e) {
            System.err.println("API Error (status " + e.getStatusCode() + "): " + e.getMessage());
        } catch (app.bloque.sdk.core.BloqueException e) {
            System.err.println("SDK Error: " + e.getMessage());
        }
    }
}
