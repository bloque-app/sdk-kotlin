package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
     * @param options Optional settings to wait for account activation
     * @return The created CardAccount
     *
     * Example usage:
     * ```kotlin
     * // Create without waiting
     * val account = session.accounts.card.create(
     *     CreateCardAccountParams(name = "My Card")
     * )
     *
     * // Create and wait for active status with ledger
     * val account = session.accounts.card.create(
     *     CreateCardAccountParams(name = "My Card"),
     *     CreateAccountOptions(waitLedger = true)
     * )
     * ```
     */
    @JvmOverloads
    fun create(
        params: CreateCardAccountParams = CreateCardAccountParams(),
        options: CreateAccountOptions? = null
    ): CardAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = buildJsonObject {
                put("create", buildJsonObject {
                    put("card_type", "VIRTUAL")
                })
            },
            metadata = buildMap {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                params.metadata?.let { putAll(it) }
            }
        )

        val response = httpClient.post<CreateAccountResponse<CardDetails>, CreateAccountRequest>(
            path = "/api/mediums/card",
            body = request
        )

        val account = mapAccountResponse(response.result.account)

        if (options?.waitLedger == true) {
            return waitForActiveStatus(account.urn, options.timeout)
        }

        return account
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
        data class CardListResponse(val accounts: List<AccountData<CardDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=card")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<CardListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a card account by URN
     *
     * @param urn Account URN
     * @return The card account
     */
    fun get(urn: String): CardAccount {
        val accounts = list(ListCardParams(urn = urn))
        return accounts.firstOrNull()
            ?: throw RuntimeException("Account not found. URN: $urn")
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): CardAccount {
        val startTime = System.currentTimeMillis()
        val pollingInterval = 2000L // 2 seconds

        while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                throw RuntimeException("Timeout waiting for account to become active. URN: $urn")
            }

            val result = list(ListCardParams(urn = urn))
            val account = result.firstOrNull()
                ?: throw RuntimeException("Account not found. URN: $urn")

            if (account.status == "active") {
                return account
            }

            if (account.status == "creation_failed") {
                throw RuntimeException("Account creation failed. URN: $urn")
            }

            Thread.sleep(pollingInterval)
        }
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
            lastFour = account.details.lastFour,
            productType = account.details.productType,
            cardType = account.details.cardType,
            detailsUrl = account.details.detailsUrl,
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
