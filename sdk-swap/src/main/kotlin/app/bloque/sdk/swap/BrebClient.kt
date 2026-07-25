package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.RetryConfig

/**
 * Client for BRE-B instant payout orders.
 *
 * BRE-B creates an irreversible payout. The request therefore disables SDK
 * retries and optionally sends the caller's idempotency key to the API.
 */
class BrebClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a BRE-B payout order (Kusama -> BRE-B COP).
     *
     * This method never retries the PUT internally. Network errors must be
     * reconciled by the caller before attempting another payout.
     */
    fun create(params: CreateBrebOrderParams): CreateBrebOrderResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")
        val orderType = params.type ?: OrderType.SRC

        val input = CreateBrebOrderInputWire(
            takerUrn = takerUrn,
            type = orderType.value,
            rateSig = params.rateSig,
            depositInformation = BrebDepositInformationWire(
                resolutionId = params.depositInformation.resolutionId
            ),
            amountSrc = if (orderType == OrderType.SRC) params.amountSrc else null,
            amountDst = if (orderType == OrderType.DST) params.amountDst else null,
            args = BrebOrderArgsWire(params.args.sourceAccountUrn),
            nodeId = params.nodeId,
            metadata = params.metadata,
            webhookUrl = params.webhookUrl
        )

        val headers = params.idempotencyKey?.let {
            mapOf("Idempotency-Key" to it)
        }
        val response = httpClient.put<CreateOrderResponseWire, CreateBrebOrderInputWire>(
            path = "/api/order",
            body = input,
            headers = headers,
            retryConfigOverride = RetryConfig.NONE,
            retryOnConnectionFailure = false
        )

        return CreateBrebOrderResult(
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
