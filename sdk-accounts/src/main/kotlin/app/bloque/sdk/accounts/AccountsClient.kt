package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
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
     * Get aggregated balances
     *
     * Retrieves aggregated balances across all accounts owned by the authenticated user.
     *
     * @param params Optional parameters with account URNs to filter
     * @return Map of asset to balance
     */
    @JvmOverloads
    fun balance(params: GetBalanceParams = GetBalanceParams()): Map<String, TokenBalance> {
        @Serializable
        data class BalanceEntry(
            val current: String,
            val pending: String
        )

        @Serializable
        data class BalanceResponse(val balance: Map<String, BalanceEntry>)

        val queryParams = params.accountUrns?.let { urns ->
            if (urns.isNotEmpty()) "?account_urns=${urns.joinToString(",")}" else ""
        } ?: ""

        val response = httpClient.get<BalanceResponse>(
            path = "/api/accounts/balances$queryParams"
        )

        return response.balance.mapValues { (_, entry) ->
            TokenBalance(
                current = entry.current,
                pending = entry.pending
            )
        }
    }

    /**
     * Get balance for a specific account
     *
     * @param params Parameters with account URN
     * @return Map of asset to balance (includes in/out details)
     */
    fun balanceByAccount(params: GetAccountBalanceParams): Map<String, TokenBalance> {
        @Serializable
        data class BalanceEntry(
            val current: String,
            val pending: String,
            @SerialName("in") val inAmount: String,
            @SerialName("out") val outAmount: String
        )

        @Serializable
        data class BalanceResponse(val balance: Map<String, BalanceEntry>)

        val response = httpClient.get<BalanceResponse>(
            path = "/api/accounts/${params.urn}/balance"
        )

        return response.balance.mapValues { (_, entry) ->
            TokenBalance(
                current = entry.current,
                pending = entry.pending,
                `in` = entry.inAmount,
                out = entry.outAmount
            )
        }
    }
}
