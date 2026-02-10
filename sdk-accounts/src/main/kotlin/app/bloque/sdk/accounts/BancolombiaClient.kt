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
 * Client for Bancolombia account operations
 */
class BancolombiaClient constructor(
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
     * Create a new Bancolombia account
     *
     * @param params Optional parameters for account creation
     * @param options Optional settings to wait for account activation
     * @return The created BancolombiaAccount
     *
     * Example usage:
     * ```kotlin
     * // Create without waiting
     * val account = session.accounts.bancolombia.create(
     *     CreateBancolombiaAccountParams(name = "My Account")
     * )
     *
     * // Create and wait for active status with ledger
     * val account = session.accounts.bancolombia.create(
     *     CreateBancolombiaAccountParams(name = "My Account"),
     *     CreateAccountOptions(waitLedger = true)
     * )
     * ```
     */
    @JvmOverloads
    fun create(
        params: CreateBancolombiaAccountParams = CreateBancolombiaAccountParams(),
        options: CreateAccountOptions? = null
    ): BancolombiaAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = JsonObject(emptyMap()),  // API requires empty object, not null
            metadata = buildJsonObject {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                params.metadata?.let { meta ->
                    if (meta is JsonObject) {
                        meta.entries.forEach { put(it.key, it.value) }
                    }
                }
            }
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateAccountResponse<BancolombiaDetails>, CreateAccountRequest>(
            path = "/api/mediums/bancolombia",
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
     * List bancolombia accounts
     *
     * @param params Optional filter parameters
     * @return List of bancolombia accounts
     */
    @JvmOverloads
    fun list(params: ListBancolombiaParams = ListBancolombiaParams()): List<BancolombiaAccount> {
        @Serializable
        data class BancolombiaListResponse(val accounts: List<AccountData<BancolombiaDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=bancolombia")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<BancolombiaListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a bancolombia account by URN
     *
     * @param urn Account URN
     * @return The bancolombia account
     */
    fun get(urn: String): BancolombiaAccount {
        @Serializable
        data class GetAccountResponse(val account: AccountData<BancolombiaDetails>)

        val response = httpClient.get<GetAccountResponse>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.account)
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): BancolombiaAccount {
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
    fun updateMetadata(params: UpdateBancolombiaMetadataParams): BancolombiaAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: JsonElement)

        val response = httpClient.patch<CreateAccountResponse<BancolombiaDetails>, UpdateMetadataRequest>(
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
    fun updateName(urn: String, name: String): BancolombiaAccount {
        return updateMetadata(UpdateBancolombiaMetadataParams(urn, mapOf("name" to name)))
    }

    /**
     * Activate account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun activate(urn: String): BancolombiaAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<BancolombiaDetails>, StatusRequest>(
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
    fun freeze(urn: String): BancolombiaAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<BancolombiaDetails>, StatusRequest>(
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
    fun disable(urn: String): BancolombiaAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<BancolombiaDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<BancolombiaDetails>): BancolombiaAccount {
        return BancolombiaAccount(
            urn = account.urn,
            id = account.id,
            referenceCode = account.details.referenceCode,
            detailsId = account.details.id,
            paymentAgreementCode = account.details.paymentAgreementCode,
            bankAccountNumber = account.details.bankAccountNumber,
            bankAccountType = account.details.bankAccountType,
            bankAccountHolderName = account.details.bankAccountHolderName,
            bankAccountHolderIdType = account.details.bankAccountHolderIdType,
            bankAccountHolderIdValue = account.details.bankAccountHolderIdValue,
            network = account.details.network,
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
