package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Client for API key management operations.
 *
 * Most of these endpoints require an authenticated user session —
 * they cannot be called with only an sk_ key. [updateOriginMetadata] is
 * the one exception: it authenticates purely via the origin's own
 * `api_key` in the request body, so it works standalone (e.g. from a
 * backend deploy script) with no session at all.
 */
class ApiKeysClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    fun list(): List<ApiKeyInfo> {
        val response = httpClient.get<ApiKeyListResponseWire>(path = "/api/api-keys")
        return response.result
    }

    fun get(id: String): ApiKeyInfo {
        val response = httpClient.get<ApiKeyResponseWire>(path = "/api/api-keys/$id")
        return response.result
    }

    fun create(params: CreateApiKeyParams): CreateApiKeyResult {
        val response = httpClient.post<CreateApiKeyResponseWire, CreateApiKeyRequestWire>(
            path = "/api/api-keys",
            body = CreateApiKeyRequestWire(
                name = params.name,
                scopes = params.scopes,
                domains = params.domains,
                expiration = params.expiration,
                metadata = params.metadata
            )
        )
        return response.result
    }

    fun exchange(params: ExchangeApiKeyParams): ExchangeApiKeyResult {
        val response = httpClient.post<ExchangeApiKeyResponseWire, ExchangeApiKeyRequestWire>(
            path = "/api/api-keys/exchange",
            body = ExchangeApiKeyRequestWire(
                key = params.key,
                scopes = params.scopes
            )
        )
        return response.result
    }

    fun revoke(id: String) {
        httpClient.delete<MessageResponseWire>(path = "/api/api-keys/$id")
    }

    fun rotate(id: String): RotateApiKeyResult {
        val response = httpClient.post<RotateApiKeyResponseWire, Map<String, String>>(
            path = "/api/api-keys/$id/rotate",
            body = emptyMap()
        )
        return response.result
    }

    /**
     * Self-service update of an origin's hosted-gate presentation/config
     * metadata (`company`, `tosGateShowHome`, `gateAccentColor`,
     * `verificationGateReturnUrlAllowlist`) — shallow-merged into the
     * origin's stored metadata. Authenticated purely by
     * [UpdateOriginMetadataParams.apiKey]; no session required.
     *
     * Only the fields you set are patched — omitted fields are left
     * untouched on the origin, they are not cleared.
     */
    fun updateOriginMetadata(params: UpdateOriginMetadataParams): UpdateOriginMetadataResult {
        val metadata = buildJsonObject {
            params.company?.let { put("company", it) }
            params.tosGateShowHome?.let { put("tos_gate_show_home", it) }
            params.gateAccentColor?.let { put("gate_accent_color", it) }
            params.verificationGateReturnUrlAllowlist?.let { list ->
                put("verification_gate_return_url_allowlist", JsonArray(list.map { JsonPrimitive(it) }))
            }
        }
        val response = httpClient.patch<UpdateOriginMetadataResponseWire, UpdateOriginMetadataRequestWire>(
            path = "/api/api-keys/origins/${params.originName}/metadata",
            body = UpdateOriginMetadataRequestWire(apiKey = params.apiKey, metadata = metadata)
        )
        return UpdateOriginMetadataResult(originName = response.originName, updated = response.updated)
    }
}
