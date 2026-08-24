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
import kotlinx.serialization.json.put

/**
 * Client for Card account operations
 */
class CardClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    private fun mapToJsonElement(map: Map<String, Any?>?): JsonElement {
        if (map == null) return JsonObject(emptyMap())
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
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
                    else -> put(key, value?.toString() ?: "")
                }
            }
        }
    }

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
            metadata = buildJsonObject {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                val metaJson = mapToJsonElement(params.metadata)
                if (metaJson is JsonObject) {
                    metaJson.entries.forEach { put(it.key, it.value) }
                }
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
        data class UpdateMetadataRequest(val metadata: JsonElement)

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
            body = UpdateMetadataRequest(mapToJsonElement(params.metadata))
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
     * Returns the same paginated shape as [AccountsClient.movements] —
     * `data`, `page_size`, `has_more`, and `next`.
     *
     * @param params Parameters with URN, asset, and optional filters
     * @return Paginated movements with data, page_size, has_more, and next token
     */
    fun movements(params: ListMovementsParams): PagedMovements {
        val queryParams = buildString {
            val parts = mutableListOf<String>()
            parts.add("asset=${params.asset}")
            params.limit?.let { parts.add("limit=$it") }
            params.before?.let { parts.add("before=$it") }
            params.after?.let { parts.add("after=$it") }
            params.reference?.let { parts.add("reference=$it") }
            params.direction?.let { parts.add("direction=$it") }
            params.collapsedView?.let { parts.add("collapsed_view=$it") }
            params.pocket?.let { parts.add("pocket=$it") }
            params.next?.let { parts.add("next=$it") }
            append("?")
            append(parts.joinToString("&"))
        }

        return httpClient.get<PagedMovements>(
            path = "/api/accounts/${params.urn}/movements$queryParams"
        )
    }

    /**
     * Delete account
     *
     * @param urn Account URN
     * @return Updated account (status "deleted")
     */
    fun delete(urn: String): CardAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<CardDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("deleted")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Tokenize this card for Apple Pay.
     *
     * @param params Card URN, Apple Pay certificates, nonce, and nonce signature
     * @return Apple Pay tokenization payload (activation_data, encrypted_pass_data, ephemeral_public_key)
     */
    fun tokenizeApple(params: TokenizeAppleCardParams): AppleCardTokenization {
        @Serializable
        data class TokenizeAppleRequest(
            val certificates: List<String>,
            val nonce: String,
            @SerialName("nonce_signature") val nonceSignature: String
        )

        @Serializable
        data class TokenizeAppleResultWire(val tokenization: AppleCardTokenization)

        @Serializable
        data class TokenizeAppleResponseWire(val result: TokenizeAppleResultWire)

        val response = httpClient.post<TokenizeAppleResponseWire, TokenizeAppleRequest>(
            path = "/api/accounts/${params.urn}/tokenize/apple",
            body = TokenizeAppleRequest(
                certificates = params.certificates,
                nonce = params.nonce,
                nonceSignature = params.nonceSignature
            )
        )

        return response.result.tokenization
    }

    /**
     * Tokenize this card for Google Pay.
     *
     * @param params Card URN, device ID, and wallet account ID
     * @return Google Pay tokenization payload (one-time passcode)
     */
    fun tokenizeGoogle(params: TokenizeGoogleCardParams): GoogleCardTokenization {
        @Serializable
        data class TokenizeGoogleRequest(
            @SerialName("device_id") val deviceId: String,
            @SerialName("wallet_account_id") val walletAccountId: String
        )

        @Serializable
        data class TokenizeGoogleResultWire(val tokenization: GoogleCardTokenization)

        @Serializable
        data class TokenizeGoogleResponseWire(val result: TokenizeGoogleResultWire)

        val response = httpClient.post<TokenizeGoogleResponseWire, TokenizeGoogleRequest>(
            path = "/api/accounts/${params.urn}/tokenize/google",
            body = TokenizeGoogleRequest(
                deviceId = params.deviceId,
                walletAccountId = params.walletAccountId
            )
        )

        return response.result.tokenization
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
