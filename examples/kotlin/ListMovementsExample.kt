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

    // Create params for listing movements
    val params = ListMovementsParams(
        "did:bloque:account:card:usr-mockUser:crd-mockCard", // account URN (mock)
        "KSM/12", // asset (required)
        10, // limit
        null, // before
        null, // after
        null, // reference
        "in" // direction (only incoming)
    )

    val movements: List<Movement> = session.accounts.movements(params)

    movements.forEach { m ->
        println("Amount: ${m.amount}")
        println("Asset: ${m.asset}")
        println("Direction: ${m.direction}")
        println("From: ${m.fromAccountId}")
        println("To: ${m.toAccountId}") // may be null
        println("Reference: ${m.reference}")
        println("Created: ${m.createdAt}")
    }
}
