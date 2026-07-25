package examples.kotlin

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.UserSession
import app.bloque.sdk.accounts.GetAccountBalanceParams
import app.bloque.sdk.accounts.TokenBalance

fun main() {
    val bloque = BloqueSDK.builder()
        .origin("bloque-root")
        .originKey("sk_test_mock_origin_key")
        .mode(Mode.SANDBOX)
        .build()

    val session: UserSession = bloque.connect("mock-user")

    println("=== Listing card accounts ===")
    val cards = session.accounts.card.list()
    println("Found ${cards.size} card accounts")

    // Get balance for a specific account
    val params = GetAccountBalanceParams("did:bloque:account:card:usr-mockUser:crd-mockCard")
    val balances: Map<String, TokenBalance> = session.accounts.balanceByAccount(params)
    balances.forEach { (asset, balance) ->
        println("Asset: $asset")
        println("  Current: ${balance.current}")
        println("  Pending: ${balance.pending}")
        println("  In: ${balance.`in`}")
        println("  Out: ${balance.out}")
    }

    // Get total user balance
    val fullBalance: Map<String, TokenBalance> = session.accounts.balance()
    fullBalance.forEach { (asset, balance) ->
        println("Asset: $asset")
        println("  Current: ${balance.current}")
        println("  Pending: ${balance.pending}")
    }
}
