package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.BrebKeyType
import app.bloque.sdk.accounts.ResolveBrebKeyParams
import app.bloque.sdk.core.Mode
import app.bloque.sdk.swap.BrebDepositInformation
import app.bloque.sdk.swap.BrebSwapArgs
import app.bloque.sdk.swap.CreateBrebOrderParams
import app.bloque.sdk.swap.FindRatesParams

/**
 * Kotlin example: BRE-B swap orders
 *
 * This example demonstrates how to resolve a BRE-B key, find rates,
 * and create a BRE-B swap order.
 */
fun main() {
    val bloque = BloqueSDK.create(
        origin = "my-app-origin",
        apiKey = "sk_test_your_api_key_here",
        mode = Mode.SANDBOX
    )

    val user = bloque.connect("nestor")

    println("=== Step 1: Resolve BRE-B Key ===")

    val resolution = user.accounts.breb.resolveKey(
        ResolveBrebKeyParams(
            keyType = BrebKeyType.PHONE,
            key = "3003348486"
        )
    )

    println("BREB resolve key response 3003348486: $resolution")

    if (resolution.error != null || resolution.data == null) {
        throw IllegalStateException(
            resolution.error?.message ?: "Failed to resolve BRE-B key"
        )
    }

    println("\n=== Step 2: Find Swap Rates ===")

    val rates = user.swap.findRates(
        FindRatesParams(
            fromAsset = "COPM/2",
            toAsset = "COP/2",
            fromMediums = listOf("kusama"),
            toMediums = listOf("breb"),
            amountSrc = "10000000"
        )
    )

    println("Swap rates result: $rates")

    if (rates.rates.isEmpty()) {
        throw IllegalStateException(
            "No swap rates available for the specified assets and mediums."
        )
    }

    println("\n=== Step 3: Create BRE-B Swap Order ===")

    val result = user.swap.breb.create(
        CreateBrebOrderParams(
            rateSig = rates.rates.first().sig,
            amountSrc = "10000000",
            depositInformation = BrebDepositInformation(
                resolutionId = resolution.data.resolutionId
            ),
            args = BrebSwapArgs(
                sourceAccountUrn = "did:bloque:account:breb:bdb6f52b-bb95-491e-92e0-18c3aff3ec03"
            )
        )
    )

    println("BREB Swap result: $result")
    println("Order ID: ${result.order.id}")
    println("Order status: ${result.order.status}")
    println("From: ${result.order.fromAmount} ${result.order.fromAsset}")
    println("To: ${result.order.toAmount} ${result.order.toAsset}")

    result.execution?.let { execution ->
        println("\nExecution:")
        println("Node ID: ${execution.nodeId}")
        println("Status: ${execution.result.status}")
        println("Description: ${execution.result.description}")
    }
}
