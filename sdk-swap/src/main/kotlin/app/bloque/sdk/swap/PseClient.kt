package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for PSE (Pagos Seguros en Línea) bank utilities
 */
class PseClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Get list of available PSE banks
     *
     * @return ListBanksResult containing list of available banks
     */
    fun banks(): ListBanksResult {
        val response = httpClient.get<ListPseBanksResponseWire>(
            path = "/api/utils/pse/banks"
        )

        return ListBanksResult(
            banks = response.banks.map { mapBankResponse(it) }
        )
    }

    /**
     * Create a PSE swap order
     *
     * Creates a swap order using PSE as the source payment medium.
     * Optionally auto-executes the first instruction node if args are provided.
     *
     * @param params PSE order parameters
     * @return CreatePseOrderResult containing the created order with optional execution result
     */
    fun create(params: CreatePseOrderParams): CreatePseOrderResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")

        val orderType = params.type ?: OrderType.SRC

        val input = CreateOrderInputWire(
            takerUrn = takerUrn,
            type = orderType.value,
            rateSig = params.rateSig,
            fromMedium = "pse",
            toMedium = params.toMedium,
            depositInformation = DepositInformationWire(urn = params.depositInformation.urn),
            amountSrc = if (orderType == OrderType.SRC) params.amountSrc else null,
            amountDst = if (orderType == OrderType.DST) params.amountDst else null,
            args = params.args?.let { mapArgsToWire(it) },
            nodeId = params.nodeId,
            metadata = params.metadata
        )

        val response = httpClient.put<CreateOrderResponseWire, CreateOrderInputWire>(
            path = "/api/order",
            body = input
        )

        return CreatePseOrderResult(
            order = mapOrderResponse(response.result.order),
            execution = response.result.execution?.let { mapExecutionResult(it) },
            requestId = response.reqId
        )
    }

    private fun mapBankResponse(wire: PseBankWire): Bank {
        return Bank(
            code = wire.financialInstitutionCode,
            name = wire.financialInstitutionName
        )
    }

    private fun mapArgsToWire(args: PseOrderArgs): PseOrderArgsWire {
        return PseOrderArgsWire(
            bankCode = args.bankCode,
            userType = args.userType,
            customerEmail = args.customerEmail,
            userLegalIdType = args.userLegalIdType,
            userLegalId = args.userLegalId,
            customerData = args.customerData?.let { CustomerDataWire(fullName = it.fullName) }
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
