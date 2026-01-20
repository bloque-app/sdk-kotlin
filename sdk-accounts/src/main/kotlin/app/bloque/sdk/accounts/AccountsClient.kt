package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.Serializable

/**
 * Main client for all account operations
 */
class AccountsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Bancolombia account operations
     */
    val bancolombia: BancolombiaClient = BancolombiaClient(httpClient)

    /**
     * Card account operations
     */
    val card: CardClient = CardClient(httpClient)

    /**
     * Virtual account operations
     */
    val virtual: VirtualClient = VirtualClient(httpClient)

    /**
     * Polygon wallet account operations
     */
    val polygon: PolygonClient = PolygonClient(httpClient)

    /**
     * Transfer funds between accounts
     *
     * @param params Transfer parameters (source, destination, amount, asset)
     * @return Transfer result with queue ID and status
     */
    fun transfer(params: TransferParams): TransferResult {
        val request = TransferRequest(
            destinationAccountUrn = params.destinationUrn,
            amount = params.amount,
            asset = params.asset.value,
            metadata = params.metadata?.mapValues { it.value.toString() } ?: emptyMap()
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<TransferResponseWrapper, TransferRequest>(
            path = "/api/accounts/${params.sourceUrn}/transfer",
            body = request,
            headers = headers
        )

        return TransferResult(
            queueId = response.result.queueId,
            status = response.result.status,
            message = response.result.message
        )
    }

    /**
     * Get account movements/transactions
     *
     * Works with any account type (card, virtual, bancolombia, polygon).
     * See: https://api.bloque.app/docs/mediums#tag/accounts/GET/api/accounts/{urn}/movements
     *
     * @param params Parameters with account URN and optional pagination
     * @return List of movements
     */
    fun movements(params: ListMovementsParams): List<Movement> {
        @Serializable
        data class MovementsResult(val movements: List<Movement>)

        @Serializable
        data class MovementsResponse(val result: MovementsResult)

        val queryParams = buildString {
            val parts = mutableListOf<String>()
            params.limit?.let { parts.add("limit=$it") }
            params.offset?.let { parts.add("offset=$it") }
            if (parts.isNotEmpty()) {
                append("?")
                append(parts.joinToString("&"))
            }
        }

        val response = httpClient.get<MovementsResponse>(
            path = "/api/accounts/${params.urn}/movements$queryParams"
        )

        return response.result.movements
    }

    /**
     * Get account balance
     *
     * Works with any account type (card, virtual, bancolombia, polygon).
     *
     * @param params Parameters with account URN and optional asset filter
     * @return Map of asset to balance
     */
    fun balance(params: GetBalanceParams): Map<String, TokenBalance> {
        @Serializable
        data class BalanceResult(val balances: Map<String, TokenBalanceWire>)

        @Serializable
        data class BalanceResponse(val result: BalanceResult)

        val queryParams = params.asset?.let { "?asset=${it.value}" } ?: ""

        val response = httpClient.get<BalanceResponse>(
            path = "/api/accounts/${params.urn}/balance$queryParams"
        )

        return response.result.balances.mapValues { (_, wire) ->
            TokenBalance(
                current = wire.current,
                pending = wire.pending,
                `in` = wire.inAmount,
                out = wire.outAmount
            )
        }
    }
}
