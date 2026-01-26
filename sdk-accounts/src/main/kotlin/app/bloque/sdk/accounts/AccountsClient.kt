package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Main client for all account operations
 */
class AccountsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    private fun mapToJsonElement(map: Map<String, Any?>?): JsonElement {
        if (map == null) return JsonObject(emptyMap())
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    is Map<*, *> -> put(key, mapToJsonElement(value as Map<String, Any?>))
                    is List<*> -> put(key, buildJsonArray {
                        value.forEach { item ->
                            when (item) {
                                is String -> add(JsonPrimitive(item))
                                is Number -> add(JsonPrimitive(item))
                                is Boolean -> add(JsonPrimitive(item))
                                is Map<*, *> -> add(mapToJsonElement(item as Map<String, Any?>))
                                else -> add(JsonPrimitive(item.toString()))
                            }
                        }
                    })
                    else -> put(key, JsonPrimitive(value?.toString() ?: ""))
                }
            }
        }
    }

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
            metadata = mapToJsonElement(params.metadata)
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
     *
     * @param params Parameters with account URN, asset, and optional filters
     * @return List of movements/transactions
     */
    fun movements(params: ListMovementsParams): List<Movement> {
        @Serializable
        data class MovementsResponse(val transactions: List<Movement>)

        val queryParams = buildString {
            val parts = mutableListOf<String>()
            parts.add("asset=${params.asset}")
            params.limit?.let { parts.add("limit=$it") }
            params.before?.let { parts.add("before=$it") }
            params.after?.let { parts.add("after=$it") }
            params.reference?.let { parts.add("reference=$it") }
            params.direction?.let { parts.add("direction=$it") }
            append("?")
            append(parts.joinToString("&"))
        }

        val response = httpClient.get<MovementsResponse>(
            path = "/api/accounts/${params.urn}/movements$queryParams"
        )

        return response.transactions
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
