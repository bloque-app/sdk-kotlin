package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.swap.FindRatesParams;
import app.bloque.sdk.swap.FindRatesResult;
import app.bloque.sdk.swap.ListBanksResult;
import app.bloque.sdk.swap.SwapRate;
import app.bloque.sdk.swap.Bank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Example: Swap Operations
 *
 * This demonstrates how to:
 * 1. Find available exchange rates (e.g. USDC to COP)
 * 2. List available PSE banks
 *
 * For order creation with webhookUrl support, see PseTopUpExample.java
 */
public class SwapExample {

    public static void main(String[] args) {
        // 1. Initialize SDK
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.PRODUCTION)
            .build();

        // 2. Connect as a user
        UserSession session = bloque.connect("nestor");

        System.out.println("=== Finding Rates ===");
        
        FindRatesParams params = new FindRatesParams(
            "COP/2",                                       // fromAsset
            "DUSD/6",                                      // toAsset
            Arrays.asList("link-pse"),                     // fromMediums
            Collections.singletonList("kusama"),           // toMediums
            "100000000",                                   // amountSrc (optional)
            null,                                          // amountDst (optional)
            null,                                          // sort (optional)
            null                                           // sortBy (optional)
        );

        FindRatesResult ratesResult = session.getSwap().findRates(params);

        System.out.println("Found " + ratesResult.getRates().size() + " rates:");
        for (SwapRate rate : ratesResult.getRates()) {
            System.out.println("- Rate ID: " + rate.getId());
            System.out.println("  Price: " + rate.getRate().getFirst() + " / " + rate.getRate().getSecond());
            System.out.println("  Maker: " + rate.getMaker());
        }

        System.out.println("\n=== Listing PSE Banks ===");

        ListBanksResult banksResult = session.getSwap().getPse().banks();
        List<Bank> banks = banksResult.getBanks();

        System.out.println("Found " + banks.size() + " banks:");
        for (Bank bank : banks) {
            System.out.println("- [" + bank.getCode() + "] " + bank.getName());
        }
    }
}
