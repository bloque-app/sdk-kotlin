package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for recurring-card subscription orders (`recurring-card` -> `kusama`).
 *
 * A recurring-card order tokenizes a card once at signup, then eager-expands
 * the schedule (via [RecurringCardDepositInformation.cron] / `allocations`)
 * into one charge -> settle -> payout sub-flow per occurrence, materialized
 * as concrete graph nodes at order-creation time. Card/3DS/customer data
 * (see [TokenizeCardArgs]) are transient signup-only `args` — never
 * persisted to `deposit_information`.
 *
 * Use [SwapClient.cancelSubscription] to halt future (not in-flight)
 * occurrences.
 *
 * Usage (Kotlin):
 * ```kotlin
 * val result = session.swap.card.createSubscription(
 *     CreateCardSubscriptionParams(
 *         rateSig = "rate_sig_abc123",
 *         amountSrc = "5000000",
 *         depositInformation = RecurringCardDepositInformation(
 *             cron = "0 0 1 * *",
 *             allocations = listOf(RecurringCardAllocation(AllocationUnit.PERCENT, 100.0)),
 *             customerEmail = "customer@example.com"
 *         ),
 *         args = TokenizeCardArgs(
 *             customerEmail = "customer@example.com",
 *             card = CardDetails(
 *                 number = "4242424242424242",
 *                 cvc = "123",
 *                 expMonth = "12",
 *                 expYear = "29",
 *                 cardHolder = "Jane Doe"
 *             ),
 *             isThreeDs = true
 *         )
 *     )
 * )
 * ```
 */
class CardClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a recurring-card subscription order.
     *
     * @param params Recurring-card subscription parameters
     * @return CreateCardSubscriptionResult containing the created order with optional execution result
     */
    fun createSubscription(params: CreateCardSubscriptionParams): CreateCardSubscriptionResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")

        val orderType = params.type ?: OrderType.SRC

        val input = CreateCardSubscriptionInputWire(
            takerUrn = takerUrn,
            type = orderType.value,
            rateSig = params.rateSig,
            depositInformation = mapDepositInfoToWire(params.depositInformation),
            amountSrc = if (orderType == OrderType.SRC) params.amountSrc else null,
            amountDst = if (orderType == OrderType.DST) params.amountDst else null,
            args = mapArgsToWire(params.args),
            nodeId = params.nodeId,
            metadata = params.metadata,
            webhookUrl = params.webhookUrl
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.put<CreateOrderResponseWire, CreateCardSubscriptionInputWire>(
            path = "/api/order",
            body = input,
            headers = headers
        )

        return CreateCardSubscriptionResult(
            order = mapOrderResponse(response.result.order),
            execution = response.result.execution?.let { mapExecutionResult(it) },
            requestId = response.reqId
        )
    }

    private fun mapDepositInfoToWire(info: RecurringCardDepositInformation): RecurringCardDepositInformationWire {
        return RecurringCardDepositInformationWire(
            cron = info.cron,
            allocations = info.allocations.map {
                RecurringCardAllocationWire(unit = it.unit.value, value = it.value, label = it.label)
            },
            customerEmail = info.customerEmail,
            startDate = info.startDate,
            trialDays = info.trialDays,
            endDate = info.endDate,
            timezone = info.timezone,
            maxOccurrences = info.maxOccurrences,
            ip = info.ip,
            paymentTransfers = info.paymentTransfers?.map {
                PaymentTransferWire(
                    creditAccountId = it.creditAccountId,
                    amount = it.amount,
                    reference = it.reference,
                    metadata = it.metadata
                )
            },
            paymentWithdrawals = info.paymentWithdrawals?.map {
                PaymentWithdrawalWire(
                    coin = it.coin,
                    address = it.address,
                    network = it.network,
                    amount = it.amount,
                    reference = it.reference
                )
            }
        )
    }

    private fun mapArgsToWire(args: TokenizeCardArgs): TokenizeCardArgsWire {
        return TokenizeCardArgsWire(
            customerEmail = args.customerEmail,
            card = args.card?.let {
                CardDetailsWire(
                    number = it.number,
                    cvc = it.cvc,
                    expMonth = it.expMonth,
                    expYear = it.expYear,
                    cardHolder = it.cardHolder
                )
            },
            cardToken = args.cardToken,
            isThreeDs = args.isThreeDs,
            browserInfo = args.browserInfo?.let {
                CardBrowserInfoWire(
                    browserColorDepth = it.browserColorDepth,
                    browserScreenHeight = it.browserScreenHeight,
                    browserScreenWidth = it.browserScreenWidth,
                    browserLanguage = it.browserLanguage,
                    browserUserAgent = it.browserUserAgent,
                    browserTz = it.browserTz
                )
            },
            threeDsAuthType = args.threeDsAuthType,
            customerData = args.customerData?.let {
                CardCustomerDataWire(phoneNumber = it.phoneNumber, fullName = it.fullName)
            }
        )
    }

    private fun mapOrderResponse(wire: OrderWire): SwapOrder {
        return SwapOrder(
            id = wire.id,
            orderSig = wire.orderSig,
            rateSig = wire.rateSig,
            swapSig = wire.swapSig,
            taker = wire.taker,
            maker = wire.maker,
            fromAsset = wire.fromAsset,
            toAsset = wire.toAsset,
            fromMedium = wire.fromMedium,
            toMedium = wire.toMedium,
            fromAmount = wire.fromAmount,
            toAmount = wire.toAmount,
            at = wire.at,
            graphId = wire.graphId,
            status = wire.status,
            metadata = wire.metadata,
            createdAt = wire.createdAt,
            updatedAt = wire.updatedAt
        )
    }

    private fun mapExecutionResult(wire: ExecutionResultWire): ExecutionResult {
        return ExecutionResult(
            nodeId = wire.nodeId,
            result = ExecutionResultDetails(
                status = wire.result.status,
                name = wire.result.name,
                description = wire.result.description,
                how = wire.result.how?.let { ExecutionHow(url = it.url) },
                callbackToken = wire.result.callbackToken
            )
        )
    }
}
