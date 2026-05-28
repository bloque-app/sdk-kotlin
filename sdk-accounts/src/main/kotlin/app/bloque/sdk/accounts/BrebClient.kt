package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueAPIError
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Client for BRE-B key account operations
 */
class BrebClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

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

    @Serializable
    private data class ResolveBrebKeyRequest(
        @SerialName("key_type") val keyType: BrebKeyType? = null,
        val key: String
    )

    @Serializable
    private data class ResolveBrebKeyResponse(
        val result: BrebResolvedKey,
        @SerialName("req_id") val requestId: String
    )

    @Serializable
    private data class StatusRequest(
        val status: String
    )

    @Serializable
    private data class ProviderErrorDetails(
        @SerialName("provider_code") val providerCode: String? = null,
        val message: String? = null
    )

    @Serializable
    private data class ProviderErrorResponse(
        @SerialName("extra_details") val extraDetails: ProviderErrorDetails? = null
    )

    private fun mapBrebAccount(account: AccountData<BrebDetails>): BrebKeyAccount {
        return BrebKeyAccount(
            id = account.id,
            urn = account.urn,
            ownerUrn = account.ownerUrn,
            remoteKeyId = account.details.remoteKeyId,
            accountId = account.details.accountId,
            keyType = account.details.key.keyType,
            key = account.details.key.keyValue,
            displayName = account.details.displayName,
            status = account.status,
            ledgerId = account.ledgerAccountId,
            webhookUrl = account.webhookUrl,
            metadata = account.metadata,
            details = account.details
        )
    }

    private fun mapError(error: Exception): BrebOperationError {
        if (error is BloqueAPIError) {
            val providerCode = error.errorBody
                ?.let { body ->
                    runCatching {
                        json.decodeFromString<ProviderErrorResponse>(body)
                    }.getOrNull()
                }
                ?.extraDetails
                ?.providerCode

            return BrebOperationError(
                code = providerCode,
                message = error.message ?: "Unknown BRE-B error"
            )
        }

        return BrebOperationError(
            code = null,
            message = error.message ?: "Unknown BRE-B error"
        )
    }

    /**
     * Create a BRE-B key account.
     */
    fun createKey(params: CreateBrebKeyParams): BrebOperationResult<BrebKeyAccount> {
        return try {
            val holderUrn = httpClient.getUrn()?.trim()
            require(!holderUrn.isNullOrEmpty()) { "Holder URN is required" }
            require(params.key.trim().isNotEmpty()) { "BRE-B key value is required" }

            val request = CreateAccountRequest(
                holderUrn = holderUrn,
                webhookUrl = params.webhookUrl,
                ledgerAccountId = params.ledgerId,
                input = buildJsonObject {
                    params.keyType?.let { put("key_type", it.name) }
                    put("key_value", params.key)
                    params.displayName?.let { put("display_name", it) }
                },
                metadata = buildJsonObject {
                    put("source", "sdk-kotlin")
                    val metaJson = mapToJsonElement(params.metadata)
                    if (metaJson is JsonObject) {
                        metaJson.entries.forEach { put(it.key, it.value) }
                    }
                }
            )

            val response = httpClient.post<CreateAccountResponse<BrebDetails>, CreateAccountRequest>(
                path = "/api/mediums/breb",
                body = request
            )

            BrebOperationResult(
                data = mapBrebAccount(response.result.account),
                error = null
            )
        } catch (error: Exception) {
            BrebOperationResult(
                data = null,
                error = mapError(error)
            )
        }
    }

    /**
     * Resolve a BRE-B key.
     */
    fun resolveKey(params: ResolveBrebKeyParams): BrebOperationResult<BrebResolvedKey> {
        return try {
            require(params.key.trim().isNotEmpty()) { "BRE-B key value is required" }

            val response = httpClient.post<ResolveBrebKeyResponse, ResolveBrebKeyRequest>(
                path = "/api/mediums/breb/resolve-key",
                body = ResolveBrebKeyRequest(
                    keyType = params.keyType,
                    key = params.key
                )
            )

            BrebOperationResult(
                data = response.result,
                error = null
            )
        } catch (error: Exception) {
            BrebOperationResult(
                data = null,
                error = mapError(error)
            )
        }
    }

    /**
     * Delete a BRE-B key account.
     */
    fun deleteKey(params: DeleteBrebKeyParams): BrebOperationResult<DeleteBrebKeyResult> {
        return try {
            require(params.accountUrn.trim().isNotEmpty()) { "BRE-B account URN is required" }

            val response = httpClient.patch<CreateAccountResponse<BrebDetails>, StatusRequest>(
                path = "/api/accounts/${params.accountUrn}",
                body = StatusRequest("deleted")
            )

            BrebOperationResult(
                data = DeleteBrebKeyResult(
                    deleted = true,
                    accountUrn = response.result.account.urn,
                    keyId = response.result.account.details.id,
                    status = "deleted"
                ),
                error = null
            )
        } catch (error: Exception) {
            BrebOperationResult(
                data = null,
                error = mapError(error)
            )
        }
    }

    /**
     * Suspend a BRE-B key account.
     */
    fun suspendKey(params: SuspendBrebKeyParams): BrebOperationResult<SuspendBrebKeyResult> {
        return try {
            require(params.accountUrn.trim().isNotEmpty()) { "BRE-B account URN is required" }

            val response = httpClient.patch<CreateAccountResponse<BrebDetails>, StatusRequest>(
                path = "/api/accounts/${params.accountUrn}",
                body = StatusRequest("frozen")
            )

            BrebOperationResult(
                data = SuspendBrebKeyResult(
                    accountUrn = response.result.account.urn,
                    keyId = response.result.account.details.id,
                    keyStatus = response.result.account.details.status,
                    status = "frozen"
                ),
                error = null
            )
        } catch (error: Exception) {
            BrebOperationResult(
                data = null,
                error = mapError(error)
            )
        }
    }

    /**
     * Activate a previously suspended BRE-B key account.
     */
    fun activateKey(params: ActivateBrebKeyParams): BrebOperationResult<ActivateBrebKeyResult> {
        return try {
            require(params.accountUrn.trim().isNotEmpty()) { "BRE-B account URN is required" }

            val response = httpClient.patch<CreateAccountResponse<BrebDetails>, StatusRequest>(
                path = "/api/accounts/${params.accountUrn}",
                body = StatusRequest("active")
            )

            BrebOperationResult(
                data = ActivateBrebKeyResult(
                    accountUrn = response.result.account.urn,
                    keyId = response.result.account.details.id,
                    keyStatus = response.result.account.details.status,
                    status = "active"
                ),
                error = null
            )
        } catch (error: Exception) {
            BrebOperationResult(
                data = null,
                error = mapError(error)
            )
        }
    }
}
