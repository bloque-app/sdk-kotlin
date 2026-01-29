package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for Colombian bank withdrawal operations
 *
 * This client handles swap orders that withdraw funds to Colombian bank accounts
 * (e.g., Bancolombia, Davivienda, etc.). It supports withdrawals from various source
 * mediums like kusama, card, virtual accounts, etc.
 *
 * Usage (Kotlin):
 * ```kotlin
 * val result = session.swap.colbank.create(
 *     CreateColBankOrderParams(
 *         rateSig = "cf38c6cb...",
 *         fromMedium = "kusama",
 *         toMedium = "bancolombia",  // Must match rate's toMediums
 *         amountSrc = "1000000",
 *         args = ColBankOrderArgs(accountUrn = "did:bloque:account:card:..."),
 *         depositInformation = ColBankDepositInformation(
 *             bankAccountType = BankAccountType.SAVINGS,
 *             bankAccountNumber = "57440088718",
 *             bankAccountHolderName = "David Barinas",
 *             bankAccountHolderIdentificationType = IdentificationType.CC,
 *             bankAccountHolderIdentificationValue = "1055228746"
 *         )
 *     )
 * )
 * ```
 *
 * Usage (Java):
 * ```java
 * CreateColBankOrderResult result = session.getSwap().getColbank().create(
 *     CreateColBankOrderParams.builder()
 *         .rateSig("cf38c6cb...")
 *         .fromMedium("kusama")
 *         .toMedium("bancolombia")  // Must match rate's toMediums
 *         .amountSrc("1000000")
 *         .accountUrn("did:bloque:account:card:...")
 *         .depositInformation(ColBankDepositInformation.builder()
 *             .bankAccountType(BankAccountType.SAVINGS)
 *             .bankAccountNumber("57440088718")
 *             .bankAccountHolderName("David Barinas")
 *             .bankAccountHolderIdentificationType(IdentificationType.CC)
 *             .bankAccountHolderIdentificationValue("1055228746")
 *             .build())
 *         .build()
 * );
 * ```
 */
class ColBankClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a Colombian bank withdrawal order
     *
     * Creates a swap order to withdraw funds to a Colombian bank account.
     * Optionally auto-executes the first instruction node if args are provided.
     *
     * @param params ColBank order parameters
     * @return CreateColBankOrderResult containing the created order with optional execution result
     */
    fun create(params: CreateColBankOrderParams): CreateColBankOrderResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")

        val orderType = params.type ?: OrderType.SRC

        val input = CreateColBankOrderInputWire(
            takerUrn = takerUrn,
            type = orderType.value,
            rateSig = params.rateSig,
            fromMedium = params.fromMedium,
            toMedium = params.toMedium,
            amountSrc = if (orderType == OrderType.SRC) params.amountSrc else null,
            amountDst = if (orderType == OrderType.DST) params.amountDst else null,
            args = params.args?.let { mapArgsToWire(it) },
            depositInformation = mapDepositInfoToWire(params.depositInformation),
            nodeId = params.nodeId,
            metadata = params.metadata
        )

        val response = httpClient.put<CreateOrderResponseWire, CreateColBankOrderInputWire>(
            path = "/api/order",
            body = input
        )

        return CreateColBankOrderResult(
            order = mapOrderResponse(response.result.order),
            execution = response.result.execution?.let { mapExecutionResult(it) },
            requestId = response.reqId
        )
    }

    private fun mapArgsToWire(args: ColBankOrderArgs): ColBankOrderArgsWire {
        return ColBankOrderArgsWire(
            accountUrn = args.accountUrn
        )
    }

    private fun mapDepositInfoToWire(info: ColBankDepositInformation): ColBankDepositInformationWire {
        return ColBankDepositInformationWire(
            bankAccountType = info.bankAccountType.value,
            bankAccountNumber = info.bankAccountNumber,
            bankAccountHolderName = info.bankAccountHolderName,
            bankAccountHolderIdentificationType = info.bankAccountHolderIdentificationType.value,
            bankAccountHolderIdentificationValue = info.bankAccountHolderIdentificationValue
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
