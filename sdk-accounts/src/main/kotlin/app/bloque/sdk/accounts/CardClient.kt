package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for Card account operations
 */
class CardClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a new Card account
     *
     * @param params Optional parameters for account creation
     * @return The created CardAccount
     */
    @JvmOverloads
    fun create(params: CreateCardAccountParams = CreateCardAccountParams()): CardAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = emptyMap(),
            metadata = buildMap {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                putAll(params.metadata)
            }
        )

        val response = httpClient.post<CreateAccountResponse<CardDetails>, CreateAccountRequest>(
            path = "/api/mediums/card",
            body = request
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * List card accounts
     *
     * @param params Optional filter parameters
     * @return List of card accounts
     */
    @JvmOverloads
    fun list(params: ListCardParams = ListCardParams()): List<CardAccount> {
        @Serializable
        data class CardListResult(val accounts: List<AccountData<CardDetails>>)

        @Serializable
        data class CardListResponse(val result: CardListResult)

        val queryParams = buildString {
            append("?")
            params.holderUrn?.let { append("holder_urn=$it&") }
            params.status?.let { append("status=$it&") }
        }.removeSuffix("&")

        val response = httpClient.get<CardListResponse>(
            path = "/api/mediums/card/accounts$queryParams"
        )

        return response.result.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Update account metadata
     *
     * @param params Parameters with URN and new metadata
     * @return Updated account
     */
    fun updateMetadata(params: UpdateCardMetadataParams): CardAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: Map<String, String?>)

        val response = httpClient.put<CreateAccountResponse<CardDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}/metadata",
            body = UpdateMetadataRequest(params.metadata)
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Update account name
     *
     * @param urn Account URN
     * @param name New name
     * @return Updated account
     */
    fun updateName(urn: String, name: String): CardAccount {
        return updateMetadata(UpdateCardMetadataParams(urn, mapOf("name" to name)))
    }

    /**
     * Activate account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun activate(urn: String): CardAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("active")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Freeze account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun freeze(urn: String): CardAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("frozen")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Disable account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun disable(urn: String): CardAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Get account movements/transactions
     *
     * @param params Parameters with URN and pagination
     * @return List of movements
     */
    fun movements(params: ListMovementsParams): List<CardMovement> {
        @Serializable
        data class CardMovementsResult(val movements: List<CardMovement>)

        @Serializable
        data class CardMovementsResponse(val result: CardMovementsResult)

        val queryParams = buildString {
            append("?")
            params.limit?.let { append("limit=$it&") }
            params.offset?.let { append("offset=$it&") }
        }.removeSuffix("&")

        val response = httpClient.get<CardMovementsResponse>(
            path = "/api/accounts/${params.urn}/movements$queryParams"
        )

        return response.result.movements
    }

    /**
     * Get account balance
     *
     * @param params Parameters with URN and optional asset filter
     * @return Map of asset to balance
     */
    fun balance(params: GetBalanceParams): Map<String, TokenBalance> {
        @Serializable
        data class CardBalanceResult(val balances: Map<String, TokenBalanceWire>)

        @Serializable
        data class CardBalanceResponse(val result: CardBalanceResult)

        val queryParams = params.asset?.let { "?asset=${it.value}" } ?: ""

        val response = httpClient.get<CardBalanceResponse>(
            path = "/api/accounts/${params.urn}/balance$queryParams"
        )

        return response.result.balances.mapValues { (_, wireBalance) ->
            TokenBalance(
                current = wireBalance.current,
                pending = wireBalance.pending,
                `in` = wireBalance.inAmount,
                out = wireBalance.outAmount
            )
        }
    }

    private fun mapAccountResponse(account: AccountData<CardDetails>): CardAccount {
        return CardAccount(
            urn = account.urn,
            id = account.id,
            cardNumber = account.details.cardNumber,
            cardHolder = account.details.cardHolder,
            expiryDate = account.details.expiryDate,
            status = account.status,
            ownerUrn = account.ownerUrn,
            ledgerId = account.ledgerAccountId,
            webhookUrl = account.webhookUrl,
            metadata = account.metadata,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
