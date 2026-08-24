package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.swap.AllocationUnit
import app.bloque.sdk.swap.CardDetails
import app.bloque.sdk.swap.CreateCardSubscriptionParams
import app.bloque.sdk.swap.FindRatesParams
import app.bloque.sdk.swap.RecurringCardAllocation
import app.bloque.sdk.swap.RecurringCardDepositInformation
import app.bloque.sdk.swap.TokenizeCardArgs

/**
 * Kotlin example: Recurring-card subscription
 *
 * This example demonstrates how to find a rate for the recurring-card rail,
 * sign up a customer for a monthly card subscription (tokenizing their card
 * at signup only — card data is never persisted), and later cancel it.
 */
fun main() {
    val bloque = BloqueSDK.createWithOriginKey(
        origin = "my-app-origin",
        originKey = "sk_test_your_origin_key_here",
        mode = Mode.SANDBOX
    )

    val user = bloque.connect("jane")

    println("=== Step 1: Find Rates for recurring-card -> kusama ===")

    val rates = user.swap.findRates(
        FindRatesParams(
            fromAsset = "USD/2",
            toAsset = "USD/2",
            fromMediums = listOf("recurring-card"),
            toMediums = listOf("kusama"),
            amountSrc = "5000000"
        )
    )

    if (rates.rates.isEmpty()) {
        throw IllegalStateException("No rates available for recurring-card -> kusama.")
    }

    println("\n=== Step 2: Create Recurring-Card Subscription ===")

    val result = user.swap.card.createSubscription(
        CreateCardSubscriptionParams(
            rateSig = rates.rates.first().sig,
            amountSrc = "5000000", // total pool amount (USD/2 cents) sliced across occurrences
            depositInformation = RecurringCardDepositInformation(
                cron = "0 0 1 * *", // monthly, 1st of the month
                allocations = listOf(RecurringCardAllocation(unit = AllocationUnit.PERCENT, value = 100.0)),
                customerEmail = "jane@example.com",
                maxOccurrences = 12
            ),
            args = TokenizeCardArgs(
                customerEmail = "jane@example.com",
                card = CardDetails(
                    number = "4242424242424242",
                    cvc = "123",
                    expMonth = "12",
                    expYear = "29",
                    cardHolder = "Jane Doe"
                ),
                // Required for unattended recurring charges (3RI) on most issuers.
                isThreeDs = true
            ),
            webhookUrl = "https://myapp.com/webhooks/order-status"
        )
    )

    println("Subscription created!")
    println("Order ID: ${result.order.id}")
    println("Order status: ${result.order.status}")

    result.execution?.let { execution ->
        println("\nExecution:")
        println("Node ID: ${execution.nodeId}")
        println("Status: ${execution.result.status}")
    }

    println("\n=== Step 3: Cancel the Subscription ===")

    val cancellation = user.swap.cancelSubscription(result.order.orderSig)
    println("Cancellation status: ${cancellation.status}")
    println("Next occurrence cursor: ${cancellation.cursor}")
}
