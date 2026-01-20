package examples.java;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.ListMovementsParams;
import app.bloque.sdk.accounts.Movement;
import java.util.List;

public class ListMovementsExample {
    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("bloque-root")
            .apiKey("sk_test_mock_api_key")
            .mode(Mode.SANDBOX)
            .build();

        UserSession session = bloque.connect("mock-user");

        // Create params for listing movements
        ListMovementsParams params = new ListMovementsParams(
            "did:bloque:account:card:usr-mockUser:crd-mockCard",
            "KSM/12", // asset (required)
            10, // limit
            null, // before
            null, // after
            null, // reference
            "in" // direction (only incoming)
        );

        List<Movement> movements = session.getAccounts().movements(params);

        movements.forEach(m -> {
            System.out.println("Amount: " + m.getAmount());
            System.out.println("Asset: " + m.getAsset());
            System.out.println("Direction: " + m.getDirection());
            System.out.println("From: " + m.getFromAccountId());
            System.out.println("To: " + m.getToAccountId()); // may be null
            System.out.println("Reference: " + m.getReference());
            System.out.println("Created: " + m.getCreatedAt());
        });
    }
}
