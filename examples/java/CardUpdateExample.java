package examples.java;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.CardAccount;
import app.bloque.sdk.accounts.UpdateCardMetadataParams;
import java.util.Map;

public class CardUpdateExample {
    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("bloque-root")
            .apiKey("sk_test_mock_api_key")
            .mode(Mode.SANDBOX)
            .build();

        UserSession session = bloque.connect("mock-user");

        System.out.println("=== Listando cuentas de tarjeta ===");
        var cards = session.getAccounts().getCard().list();
        System.out.println("Encontradas " + cards.size() + " cuentas de tarjeta");

        if (cards.isEmpty()) {
            System.out.println("No hay cuentas de tarjeta. Creando una...");
            CardAccount newCard = session.getAccounts().getCard().create();
            System.out.println("Tarjeta creada: " + newCard.getUrn());
            cards = session.getAccounts().getCard().list();
        }

        CardAccount card = cards.get(0);
        String cardUrn = card.getUrn();
        System.out.println("Usando tarjeta: " + cardUrn);
        System.out.println("Estado actual: " + card.getStatus());

        // Test updateMetadata
        System.out.println("\n=== Probando updateMetadata ===");
        UpdateCardMetadataParams metadataParams = new UpdateCardMetadataParams(
            cardUrn,
            Map.of("name", "Mi Tarjeta Actualizada", "custom_field", "valor_custom")
        );
        CardAccount updatedCard = session.getAccounts().getCard().updateMetadata(metadataParams);
        System.out.println("Metadata actualizada: " + updatedCard.getMetadata());

        // Test updateName
        System.out.println("\n=== Probando updateName ===");
        updatedCard = session.getAccounts().getCard().updateName(cardUrn, "Tarjeta Principal");
        System.out.println("Nombre actualizado. Metadata: " + updatedCard.getMetadata());

        // Test freeze
        System.out.println("\n=== Probando freeze ===");
        updatedCard = session.getAccounts().getCard().freeze(cardUrn);
        System.out.println("Tarjeta congelada. Estado: " + updatedCard.getStatus());

        // Test activate
        System.out.println("\n=== Probando activate ===");
        updatedCard = session.getAccounts().getCard().activate(cardUrn);
        System.out.println("Tarjeta activada. Estado: " + updatedCard.getStatus());

        // Test disable
        System.out.println("\n=== Probando disable ===");
        updatedCard = session.getAccounts().getCard().disable(cardUrn);
        System.out.println("Tarjeta deshabilitada. Estado: " + updatedCard.getStatus());

        System.out.println("\n=== Todas las pruebas completadas ===");
    }
}
