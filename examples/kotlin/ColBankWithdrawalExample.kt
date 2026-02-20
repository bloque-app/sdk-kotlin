package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.swap.*

/**
 * Kotlin example: Colombian Bank Withdrawal (Bancolombia)
 *
 * This example demonstrates how to withdraw funds to a Colombian bank account
 * using the Bloque SDK.
 */
fun main() {
    // Initialize the SDK
    val bloque = BloqueSDK.create(
        origin = "my-app-origin",
        apiKey = "sk_test_your_api_key_here",
        mode = Mode.SANDBOX
    )

    // Connect to a user session
    val session = bloque.connect("david")

    // ============================================
    // Step 1: Find available rates for withdrawal
    // ============================================
    println("=== Step 1: Finding Rates ===")

    val rates = session.swap.findRates(
        FindRatesParams(
            fromAsset = "COPM/2",
            toAsset = "COP/2",
            fromMediums = listOf("kusama"),
            toMediums = listOf("bancolombia"),
            amountSrc = "10000000"  // 100k
        )
    )

    if (rates.rates.isEmpty()) {
        println("No rates available for this swap")
        return
    }

    val rate = rates.rates.first()
    println("Found rate: ${rate.ratio}")
    println("Rate signature: ${rate.sig}")

    // ============================================
    // Step 2: Create withdrawal order (simple)
    // ============================================
    println("\n=== Step 2: Create Withdrawal Order ===")

    // Use the toMediums from the rate to know which banks are available
    val availableBanks = rate.toMediums
    println("Available destination banks: $availableBanks")

    val result = session.swap.colbank.create(
        CreateColBankOrderParams(
            rateSig = rate.sig,
            fromMedium = "kusama",
            toMedium = "bancolombia",  // Must be one of rate.toMediums
            amountSrc = "10000000",  // Amount in smallest unit
            type = OrderType.SRC,
            args = ColBankOrderArgs(
                accountUrn = "did:bloque:account:card:usr-xxxxx:crd-xxxxx"
            ),
            depositInformation = ColBankDepositInformation(
                bankAccountType = BankAccountType.SAVINGS,
                bankAccountNumber = "57440088718",
                bankAccountHolderName = "David Barinas",
                bankAccountHolderIdentificationType = IdentificationType.CC,
                bankAccountHolderIdentificationValue = "1055228746"
            )
        )
    )

    println("Order created!")
    println("Order ID: ${result.order.id}")
    println("Order Status: ${result.order.status}")
    println("From: ${result.order.fromAmount} ${result.order.fromAsset}")
    println("To: ${result.order.toAmount} ${result.order.toAsset}")

    result.execution?.let { exec ->
        println("\nExecution Result:")
        println("  Node ID: ${exec.nodeId}")
        println("  Status: ${exec.result.status}")
        println("  Description: ${exec.result.description}")
    }

    // ============================================
    // Step 3: Create withdrawal order (using builders)
    // ============================================
    println("\n=== Step 3: Using Builder Pattern ===")

    val builderResult = session.swap.colbank.create(
        CreateColBankOrderParams.builder()
            .rateSig(rate.sig)
            .fromMedium("kusama")
            .toMedium("bancolombia")  // Specify destination bank
            .amountSrc("5000000")
            .type(OrderType.SRC)
            .accountUrn("did:bloque:account:polygon:usr-xxxxx:pol-xxxxx")
            .depositInformation(
                ColBankDepositInformation.builder()
                    .bankAccountType(BankAccountType.SAVINGS)
                    .bankAccountNumber("57440088718")
                    .bankAccountHolderName("David Barinas")
                    .bankAccountHolderIdentificationType(IdentificationType.CC)
                    .bankAccountHolderIdentificationValue("1055228746")
                    .build()
            )
            .build()
    )

    println("Builder Order created!")
    println("Order ID: ${builderResult.order.id}")

    // ============================================
    // Step 4: Withdrawal from Card account
    // ============================================
    println("\n=== Step 4: Withdrawal from Card Account ===")

    // First find rates for card -> bancolombia
    val cardRates = session.swap.findRates(
        FindRatesParams(
            fromAsset = "COPM/2",
            toAsset = "COP/2",
            fromMediums = listOf("kusama"),
            toMediums = listOf("bancolombia"),
            amountSrc = "10000000"  // 100k
        )
    )

    if (cardRates.rates.isNotEmpty()) {
        val cardRate = cardRates.rates.first()

        val cardWithdrawal = session.swap.colbank.create(
            CreateColBankOrderParams(
                rateSig = cardRate.sig,
                fromMedium = "kusama",
                toMedium = "bancolombia",
                amountSrc = "10000000",
                args = ColBankOrderArgs(
                    accountUrn = "did:bloque:account:card:usr-xxxxx:crd-xxxxx"
                ),
                depositInformation = ColBankDepositInformation(
                    bankAccountType = BankAccountType.CHECKINGS,
                    bankAccountNumber = "12345678901",
                    bankAccountHolderName = "Mi Empresa SAS",
                    bankAccountHolderIdentificationType = IdentificationType.NIT,
                    bankAccountHolderIdentificationValue = "900123456"
                ),
                metadata = mapOf(
                    "reference" to "withdrawal-001",
                    "note" to "Monthly withdrawal"
                )
            )
        )

        println("Card withdrawal order created!")
        println("Order ID: ${cardWithdrawal.order.id}")
    }

    // ============================================
    // Step 5: Create order with webhook notifications
    // ============================================
    println("\n=== Step 5: Create Order with Webhook ===")

    val webhookResult = session.swap.colbank.create(
        CreateColBankOrderParams(
            rateSig = rate.sig,
            fromMedium = "kusama",
            toMedium = "bancolombia",
            amountSrc = "10000000",
            type = OrderType.SRC,
            args = ColBankOrderArgs(
                accountUrn = "did:bloque:account:card:usr-xxxxx:crd-xxxxx"
            ),
            depositInformation = ColBankDepositInformation(
                bankAccountType = BankAccountType.SAVINGS,
                bankAccountNumber = "57440088718",
                bankAccountHolderName = "David Barinas",
                bankAccountHolderIdentificationType = IdentificationType.CC,
                bankAccountHolderIdentificationValue = "1055228746"
            ),
            webhookUrl = "https://myapp.com/webhooks/order-status"
        )
    )

    println("Order with webhook created!")
    println("Order ID: ${webhookResult.order.id}")
    println("Status updates will be sent to: https://myapp.com/webhooks/order-status")

    // ============================================
    // Step 6: Specify destination amount (DST type)
    // ============================================
    println("\n=== Step 6: Specify Destination Amount ===")

    val dstResult = session.swap.colbank.create(
        CreateColBankOrderParams(
            rateSig = rate.sig,
            fromMedium = "kusama",
            toMedium = "bancolombia",
            amountDst = "500000000",  // 5,000,000 COP (in smallest unit, 2 decimals)
            type = OrderType.DST,
            args = ColBankOrderArgs(
                accountUrn = "did:bloque:account:polygon:usr-xxxxx:pol-xxxxx"
            ),
            depositInformation = ColBankDepositInformation(
                bankAccountType = BankAccountType.SAVINGS,
                bankAccountNumber = "57440088718",
                bankAccountHolderName = "David Barinas",
                bankAccountHolderIdentificationType = IdentificationType.CC,
                bankAccountHolderIdentificationValue = "1055228746"
            )
        )
    )

    println("DST Order created!")
    println("Will receive: ${dstResult.order.toAmount} COP")
    println("Will pay: ${dstResult.order.fromAmount} COPM")

    // ============================================
    // Step 7: List orders as taker
    // ============================================
    println("\n=== Step 7: List Orders ===")

    // List all orders
    val allOrders = session.swap.listOrders()
    println("Total orders: ${allOrders.orders.size}")

    // List only pending orders
    val pendingOrders = session.swap.listOrders(
        ListOrdersParams(status = OrderStatus.PENDING)
    )
    println("Pending orders: ${pendingOrders.orders.size}")

    // List completed orders
    val completedOrders = session.swap.listOrders(
        ListOrdersParams(status = OrderStatus.COMPLETED)
    )
    println("Completed orders: ${completedOrders.orders.size}")

    // List orders with multiple filters
    val filteredOrders = session.swap.listOrders(
        ListOrdersParams(
            status = OrderStatus.COMPLETED,
            after = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L  // Last 7 days
        )
    )
    println("Completed orders in last 7 days: ${filteredOrders.orders.size}")

    // Print order details
    allOrders.orders.take(3).forEach { order ->
        println("\nOrder ID: ${order.id}")
        println("  Status: ${order.status}")
        println("  From: ${order.fromAmount} ${order.fromAsset} (${order.fromMedium})")
        println("  To: ${order.toAmount} ${order.toAsset} (${order.toMedium})")
        println("  Created: ${order.createdAt}")
    }

    println("\n=== All Examples Completed ===")
}
