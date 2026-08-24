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
 * Client for `us2-account` operations — a fiat virtual account medium that is
 * KYC-gated, provisions a dynamic EVM deposit wallet, and settles deposits
 * via swap. Unlike [UsAccountClient] (Bridge), the holder's profile fields
 * (name, address, tax id, ...) are supplied directly at creation time.
 */
class Us2AccountClient constructor(
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
     * Create a new us2-account.
     *
     * @param params Holder profile fields and optional account-creation parameters
     * @param options Optional settings to wait for account activation
     * @return The created Us2Account
     */
    @JvmOverloads
    fun create(
        params: CreateUs2AccountParams,
        options: CreateAccountOptions? = null
    ): Us2Account {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = buildJsonObject {
                put("type", params.type)
                put("email", params.email)
                params.name?.let { put("name", it) }
                params.firstName?.let { put("first_name", it) }
                params.lastName?.let { put("last_name", it) }
                params.middleName?.let { put("middle_name", it) }
                params.companyName?.let { put("company_name", it) }
                params.incorporationDate?.let { put("incorporation_date", it) }
                params.ein?.let { put("ein", it) }
                params.phone?.let { put("phone", it) }
                params.address?.let { address ->
                    put("address", buildJsonObject {
                        put("street", address.street)
                        put("city", address.city)
                        put("state", address.state)
                        put("postal_code", address.postalCode)
                        put("country", address.country)
                    })
                }
                params.taxId?.let { put("tax_id", it) }
                params.proofOfAddress?.let { put("proof_of_address", it) }
                params.businessFormationDocument?.let { put("business_formation_document", it) }
            },
            metadata = buildJsonObject {
                put("source", "sdk-kotlin")
                val metaJson = mapToJsonElement(params.metadata)
                if (metaJson is JsonObject) {
                    metaJson.entries.forEach { put(it.key, it.value) }
                }
            }
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateAccountResponse<Us2AccountDetails>, CreateAccountRequest>(
            path = "/api/mediums/us2-account",
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
     * List us2-accounts.
     *
     * @param params Optional filter parameters
     * @return List of us2-accounts
     */
    @JvmOverloads
    fun list(params: ListUs2AccountParams = ListUs2AccountParams()): List<Us2Account> {
        @Serializable
        data class Us2AccountListResponse(val accounts: List<AccountData<Us2AccountDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=us2-account")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<Us2AccountListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a us2-account by URN.
     *
     * @param urn Account URN
     * @return The us2-account
     */
    fun get(urn: String): Us2Account {
        @Serializable
        data class GetAccountResponse(val account: AccountData<Us2AccountDetails>)

        val response = httpClient.get<GetAccountResponse>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.account)
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): Us2Account {
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
    fun updateMetadata(params: UpdateUs2AccountMetadataParams): Us2Account {
        @Serializable
        data class UpdateMetadataRequest(val metadata: JsonElement)

        val response = httpClient.patch<CreateAccountResponse<Us2AccountDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
            body = UpdateMetadataRequest(mapToJsonElement(params.metadata))
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Activate account
     */
    fun activate(urn: String): Us2Account {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<Us2AccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("active")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Freeze account
     */
    fun freeze(urn: String): Us2Account {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<Us2AccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("frozen")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Disable account
     */
    fun disable(urn: String): Us2Account {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<Us2AccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Delete account
     */
    fun delete(urn: String): Us2Account {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<Us2AccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("deleted")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<Us2AccountDetails>): Us2Account {
        return Us2Account(
            urn = account.urn,
            id = account.id,
            userId = account.details.userId,
            virtualAccountId = account.details.virtualAccountId,
            type = account.details.type,
            currency = account.details.currency,
            sourceDepositInstructions = account.details.sourceDepositInstructions,
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
