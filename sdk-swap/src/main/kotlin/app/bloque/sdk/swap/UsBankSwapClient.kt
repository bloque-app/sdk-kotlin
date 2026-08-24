package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for US-bank ACH-pull orders (`external-us-bank` -> `kusama`, USD -> DUSD).
 *
 * This is a direct ACH-pull flow: no escrow, no Binance/USDC bridge. The
 * order resolves the caller's linked US bank into a Brale account/address,
 * pulls the ACH debit, and teleports the minted DUSD directly to the
 * taker's Kreivo ledger account.
 *
 * Usage (Kotlin):
 * ```kotlin
 * val result = session.swap.usBank.create(
 *     CreateUsBankSwapOrderParams(
 *         rateSig = "rate_sig_abc123",
 *         depositInformation = UsBankDepositInformation(ledgerAccountId = "0x1a2b3c4d5e6f7890"),
 *         args = UsBankSwapArgs(
 *             accountUrn = "did:bloque:account:external-us-bank:...",
 *             expectedOwnerUrn = "did:bloque:test-origin:user-456"
 *         ),
 *         amountSrc = "10050"
 *     )
 * )
 * ```
 */
class UsBankSwapClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a US-bank ACH-pull order.
     *
     * @param params US-bank order parameters
     * @return CreateUsBankSwapOrderResult containing the created order with optional execution result
     */
    fun create(params: CreateUsBankSwapOrderParams): CreateUsBankSwapOrderResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")

        val orderType = params.type ?: OrderType.SRC

        val input = CreateUsBankSwapOrderInputWire(
            takerUrn = takerUrn,
            type = orderType.value,
            rateSig = params.rateSig,
            depositInformation = UsBankDepositInformationWire(
                ledgerAccountId = params.depositInformation.ledgerAccountId
            ),
            amountSrc = if (orderType == OrderType.SRC) params.amountSrc else null,
            amountDst = if (orderType == OrderType.DST) params.amountDst else null,
            args = UsBankSwapArgsWire(
                accountUrn = params.args.accountUrn,
                expectedOwnerUrn = params.args.expectedOwnerUrn
            ),
            nodeId = params.nodeId,
            metadata = params.metadata,
            webhookUrl = params.webhookUrl
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.put<CreateOrderResponseWire, CreateUsBankSwapOrderInputWire>(
            path = "/api/order",
            body = input,
            headers = headers
        )

        return CreateUsBankSwapOrderResult(
            order = mapOrderResponse(response.result.order),
            execution = response.result.execution?.let { mapExecutionResult(it) },
            requestId = response.reqId
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
