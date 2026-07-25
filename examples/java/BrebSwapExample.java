package app.bloque.examples;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.BrebKeyType;
import app.bloque.sdk.accounts.BrebOperationResult;
import app.bloque.sdk.accounts.BrebResolvedKey;
import app.bloque.sdk.accounts.ResolveBrebKeyParams;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.swap.BrebDepositInformation;
import app.bloque.sdk.swap.BrebOrderArgs;
import app.bloque.sdk.swap.CreateBrebOrderParams;
import app.bloque.sdk.swap.CreateBrebOrderResult;
import app.bloque.sdk.swap.FindRatesParams;
import app.bloque.sdk.swap.FindRatesResult;

import java.util.Arrays;
import java.util.Collections;

/**
 * Java example: BRE-B swap orders
 *
 * This example demonstrates how to resolve a BRE-B key, find rates,
 * and create a BRE-B swap order.
 */
public class BrebSwapExample {

    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")
            .apiKey("sk_test_your_api_key_here")
            .mode(Mode.SANDBOX)
            .build();

        UserSession user = bloque.connect("nestor");

        System.out.println("=== Step 1: Resolve BRE-B Key ===");

        BrebOperationResult<BrebResolvedKey> resolution = user.getAccounts()
            .getBreb()
            .resolveKey(new ResolveBrebKeyParams(BrebKeyType.PHONE, "3003348486"));

        System.out.println("BREB resolve key response 3003348486: " + resolution);

        if (resolution.getError() != null || resolution.getData() == null) {
            throw new IllegalStateException(
                resolution.getError() != null
                    ? resolution.getError().getMessage()
                    : "Failed to resolve BRE-B key"
            );
        }

        System.out.println("\n=== Step 2: Find Swap Rates ===");

        FindRatesResult rates = user.getSwap().findRates(
            new FindRatesParams(
                "COPM/2",
                "COP/2",
                Collections.singletonList("kusama"),
                Collections.singletonList("breb"),
                "10000000",
                null,
                null,
                null
            )
        );

        System.out.println("Swap rates result: " + rates);

        if (rates.getRates().isEmpty()) {
            throw new IllegalStateException(
                "No swap rates available for the specified assets and mediums."
            );
        }

        System.out.println("\n=== Step 3: Create BRE-B Swap Order ===");

        CreateBrebOrderResult result = user.getSwap()
            .getBreb()
            .create(new CreateBrebOrderParams(
                rates.getRates().get(0).getSig(),
                new BrebDepositInformation(resolution.getData().getResolutionId()),
                new BrebOrderArgs("did:bloque:account:breb:bdb6f52b-bb95-491e-92e0-18c3aff3ec03"),
                "10000000",
                null,
                null,
                null,
                null,
                null,
                null
            ));

        System.out.println("BREB Swap result: " + result);
        System.out.println("Order ID: " + result.getOrder().getId());
        System.out.println("Order status: " + result.getOrder().getStatus());
        System.out.println("From: " + result.getOrder().getFromAmount() + " " + result.getOrder().getFromAsset());
        System.out.println("To: " + result.getOrder().getToAmount() + " " + result.getOrder().getToAsset());

        if (result.getExecution() != null) {
            System.out.println("\nExecution:");
            System.out.println("Node ID: " + result.getExecution().getNodeId());
            System.out.println("Status: " + result.getExecution().getResult().getStatus());
            System.out.println("Description: " + result.getExecution().getResult().getDescription());
        }
    }
}
