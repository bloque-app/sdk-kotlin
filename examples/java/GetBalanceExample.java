package examples.java;

import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.GetAccountBalanceParams;
import app.bloque.sdk.accounts.TokenBalance;
import java.util.Map;

public class GetBalanceExample {
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

        // Get balance for a specific account
        GetAccountBalanceParams params = new GetAccountBalanceParams("did:bloque:account:card:usr-mockUser:crd-mockCard");
        Map<String, TokenBalance> balances = session.getAccounts().balanceByAccount(params);
        balances.forEach((asset, balance) -> {
            System.out.println("Asset: " + asset);
            System.out.println("  Current: " + balance.getCurrent());
            System.out.println("  Pending: " + balance.getPending());
            System.out.println("  In: " + balance.getIn());
            System.out.println("  Out: " + balance.getOut());
        });

        // Get total user balance
        Map<String, TokenBalance> fullBalance = session.getAccounts().balance();
        fullBalance.forEach((asset, balance) -> {
            System.out.println("Asset: " + asset);
            System.out.println("  Current: " + balance.getCurrent());
            System.out.println("  Pending: " + balance.getPending());
        });
    }
}
