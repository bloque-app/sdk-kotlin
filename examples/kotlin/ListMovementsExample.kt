package examples.kotlin

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.UserSession
import app.bloque.sdk.accounts.ListMovementsParams
import app.bloque.sdk.accounts.Movement

fun main() {
    val bloque = BloqueSDK.builder()
        .origin("bloque-root")
        .apiKey("sk_test_mock_api_key")
        .mode(Mode.SANDBOX)
        .build()

    val session: UserSession = bloque.connect("mock-user")

    val accountUrn = "did:bloque:account:card:usr-mockUser:crd-mockCard"

    // ============================================
    // Example 1: List movements (standard)
    // ============================================
    println("=== Standard movements ===")

    val movements = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            limit = 10,
            direction = "in"
        )
    )

    movements.forEach { m ->
        println("  ${m.direction} | ${m.amount} ${m.asset} | ref=${m.reference} | ${m.createdAt}")
    }

    // ============================================
    // Example 2: List movements with collapsed view
    // ============================================
    println("\n=== Collapsed view (pending + confirmed merged) ===")

    val collapsed = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            collapsedView = true
        )
    )

    collapsed.forEach { m ->
        println("  ${m.direction} | ${m.amount} ${m.asset} | ref=${m.reference} | ${m.createdAt}")
    }
}
