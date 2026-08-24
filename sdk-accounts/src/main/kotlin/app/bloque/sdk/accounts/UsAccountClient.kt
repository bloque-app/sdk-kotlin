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
 * Client for `us-account` (Bridge-backed) account operations.
 *
 * A us-account is a US compliance account requiring Bridge Terms of Service
 * acceptance before creation — call [tosLink] first to obtain a
 * `signed_agreement_id`, then pass it to [create].
 */
class UsAccountClient constructor(
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
     * Request a Bridge Terms of Service acceptance link.
     *
     * Flow: call this to get a URL, display it to the user (iFrame or new
     * browser window), the user accepts ToS on Bridge's hosted page, and (if
     * [UsAccountTosLinkParams.redirectUri] is set) is redirected back with
     * `signed_agreement_id` as a query parameter — pass that to [create].
     *
     * @param params Optional redirect URI
     * @return URL to display to the user for ToS acceptance
     */
    @JvmOverloads
    fun tosLink(params: UsAccountTosLinkParams = UsAccountTosLinkParams()): UsAccountTosLink {
        val queryParams = params.redirectUri?.let { "?redirect_uri=$it" } ?: ""

        val response = httpClient.post<UsAccountTosLinkResponseWire, EmptyRequestBody>(
            path = "/api/mediums/us-account/tos-link$queryParams",
            body = EmptyRequestBody()
        )

        return UsAccountTosLink(url = response.result.url)
    }

    /**
     * Create a new us-account.
     *
     * The holder's name/address/etc. come from their verified identity
     * profile, not this call — only the signed ToS agreement (and optionally
     * a government ID image) are supplied here. See [tosLink].
     *
     * @param params Signed agreement id (from [tosLink]) plus optional parameters
     * @param options Optional settings to wait for account activation
     * @return The created UsAccount
     */
    @JvmOverloads
    fun create(
        params: CreateUsAccountParams,
        options: CreateAccountOptions? = null
    ): UsAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = buildJsonObject {
                put("signed_agreement_id", params.signedAgreementId)
                params.govIdImageFront?.let { put("gov_id_image_front", it) }
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

        val response = httpClient.post<CreateAccountResponse<UsAccountDetails>, CreateAccountRequest>(
            path = "/api/mediums/us-account",
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
     * List us-accounts.
     *
     * @param params Optional filter parameters
     * @return List of us-accounts
     */
    @JvmOverloads
    fun list(params: ListUsAccountParams = ListUsAccountParams()): List<UsAccount> {
        @Serializable
        data class UsAccountListResponse(val accounts: List<AccountData<UsAccountDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=us-account")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<UsAccountListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a us-account by URN.
     *
     * @param urn Account URN
     * @return The us-account
     */
    fun get(urn: String): UsAccount {
        @Serializable
        data class GetAccountResponse(val account: AccountData<UsAccountDetails>)

        val response = httpClient.get<GetAccountResponse>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.account)
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): UsAccount {
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
    fun updateMetadata(params: UpdateUsAccountMetadataParams): UsAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: JsonElement)

        val response = httpClient.patch<CreateAccountResponse<UsAccountDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
            body = UpdateMetadataRequest(mapToJsonElement(params.metadata))
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Activate account
     */
    fun activate(urn: String): UsAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<UsAccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("active")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Freeze account
     */
    fun freeze(urn: String): UsAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<UsAccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("frozen")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Disable account
     */
    fun disable(urn: String): UsAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<UsAccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Delete account
     */
    fun delete(urn: String): UsAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<UsAccountDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("deleted")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<UsAccountDetails>): UsAccount {
        return UsAccount(
            urn = account.urn,
            id = account.id,
            customerId = account.details.customerId,
            virtualAccountId = account.details.virtualAccountId,
            evmAddress = account.details.evmAddress,
            currency = account.details.currency,
            bankName = account.details.bankName,
            bankAddress = account.details.bankAddress,
            bankRoutingNumber = account.details.bankRoutingNumber,
            bankAccountNumber = account.details.bankAccountNumber,
            bankBeneficiaryName = account.details.bankBeneficiaryName,
            bankBeneficiaryAddress = account.details.bankBeneficiaryAddress,
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
