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

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateAccountResponse<CardDetails>, CreateAccountRequest>(
            path = "/api/mediums/card",
            body = request,
            headers = headers
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
            val parts = mutableListOf<String>()
            parts.add("medium=card")
            holderUrn?.let { parts.add("holder_urn=$it") }
            params.urn?.let { parts.add("urn=$it") }
            append("?")
            append(parts.joinToString("&"))
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
        @Serializable
        data class GetAccountResponse(val account: AccountData<CardDetails>)

        val response = httpClient.get<GetAccountResponse>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.account)
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

            val account = get(urn)

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

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
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

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
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

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
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

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Get account movements/transactions
     *
     * @param params Parameters with URN, asset, and optional filters
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
