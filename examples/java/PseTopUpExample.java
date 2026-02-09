package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.swap.Bank;
import app.bloque.sdk.swap.CreatePseOrderParams;
import app.bloque.sdk.swap.CreatePseOrderResult;
import app.bloque.sdk.swap.CustomerData;
import app.bloque.sdk.swap.DepositInformation;
import app.bloque.sdk.swap.FindRatesParams;
import app.bloque.sdk.swap.FindRatesResult;
import app.bloque.sdk.swap.ListBanksResult;
import app.bloque.sdk.swap.PseOrderArgs;
import app.bloque.sdk.swap.SwapRate;

import java.util.Arrays;
import java.util.Collections;

/**
 * Example: PSE Top-Up (Recharge)
 *
 * This demonstrates how to:
 * 1. Find available PSE banks
 * 2. Find exchange rates for PSE
 * 3. Create a PSE swap order for top-up/recharge
 */
public class PseTopUpExample {

    public static void main(String[] args) {
        // 1. Initialize SDK
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.PRODUCTION)
            .build();

        // 2. Connect as a user
        UserSession session = bloque.connect("user-id");

        // 3. List available PSE banks
        System.out.println("=== Available PSE Banks ===");
        ListBanksResult banksResult = session.getSwap().getPse().banks();

        if (banksResult.getBanks().isEmpty()) {
            System.out.println("No PSE banks available");
            return;
        }

        for (Bank bank : banksResult.getBanks()) {
            System.out.println("- [" + bank.getCode() + "] " + bank.getName());
        }

        // Use the first available bank
        Bank selectedBank = banksResult.getBanks().get(0);
        System.out.println("\nUsing bank: " + selectedBank.getName() + " (" + selectedBank.getCode() + ")");

        // 4. Find rates for PSE to Kusama
        System.out.println("\n=== Finding PSE Rates ===");

        FindRatesParams ratesParams = new FindRatesParams(
            "COP/2",                          // fromAsset
            "COPM/2",                          // toAsset
            Arrays.asList("pse"),              // fromMediums
            Collections.singletonList("kusama"), // toMediums
            "1000000",                         // amountSrc (10.000 COP)
            null,                              // amountDst
            null,                              // sort
            null                               // sortBy
        );

        FindRatesResult ratesResult = session.getSwap().findRates(ratesParams);

        if (ratesResult.getRates().isEmpty()) {
            System.out.println("No rates available");
            return;
        }

        SwapRate selectedRate = ratesResult.getRates().get(0);
        System.out.println("Selected rate: " + selectedRate.getSig());
        System.out.println("Rate: " + selectedRate.getRate().getFirst() + " / " + selectedRate.getRate().getSecond());

        // 5. Create PSE order with customer information
        System.out.println("\n=== Creating PSE Top-Up Order ===");

        CreatePseOrderParams orderParams = new CreatePseOrderParams(
            selectedRate.getSig(),             // rateSig
            "kusama",                          // toMedium
            new DepositInformation(            // depositInformation
                "did:bloque:account:card:usr-xxx:crd-xxx"
            ),
            "1000000",                         // amountSrc
            null,                              // amountDst
            null,                              // type (defaults to SRC)
            new PseOrderArgs(                  // args
                selectedBank.getCode(),        // bankCode (from banks list)
                "natural",                     // userType
                "user@example.com",            // customerEmail
                "CC",                          // userLegalIdType
                "123456789",                   // userLegalId
                new CustomerData("John Doe", "3012448426") // customerData (name, phone)
            ),
            null,                              // nodeId
            null                               // metadata
        );

        CreatePseOrderResult result = session.getSwap().getPse().create(orderParams);

        System.out.println("Order created successfully!");
        System.out.println("- Order ID: " + result.getOrder().getId());
        System.out.println("- Order Sig: " + result.getOrder().getOrderSig());
        System.out.println("- Status: " + result.getOrder().getStatus());
        System.out.println("- From: " + result.getOrder().getFromAmount() + " " + result.getOrder().getFromAsset());
        System.out.println("- To: " + result.getOrder().getToAmount() + " " + result.getOrder().getToAsset());

        // 6. Get checkout URL if available
        if (result.getExecution() != null && result.getExecution().getResult().getHow() != null) {
            String checkoutUrl = result.getExecution().getResult().getHow().getUrl();
            if (checkoutUrl != null) {
                System.out.println("\n=== Checkout URL ===");
                System.out.println("Redirect user to: " + checkoutUrl);
            }
        }
    }
}
