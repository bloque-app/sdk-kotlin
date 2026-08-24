package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.BloqueNotFoundError

/**
 * Client for API key management operations. All of these endpoints
 * require an authenticated user session — they cannot be called with
 * only an sk_ key.
 */
class ApiKeysClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    fun list(): List<ApiKeyInfo> {
        return httpClient.get<List<ApiKeyInfo>>(path = "/api/api-keys")
    }

    /**
     * Get a single API key by ID.
     *
     * The API returns HTTP 200 with `{statusCode: 404, ...}` instead of a
     * real 404 status when the key doesn't exist
     * (`api-key.controller.ts:305`) — this method detects that shape and
     * throws [BloqueNotFoundError] itself so callers get a real exception.
     */
    fun get(id: String): ApiKeyInfo {
        val response = httpClient.get<ApiKeyOrErrorWire>(path = "/api/api-keys/$id")

        if (response.keyId == null) {
            val message = response.message ?: "API key not found"
            throw BloqueNotFoundError(
                statusCode = response.statusCode ?: 404,
                errorBody = """{"statusCode":${response.statusCode ?: 404},"message":"$message"}""",
                message = message
            )
        }

        return ApiKeyInfo(
            id = requireNotNull(response.id),
            keyId = response.keyId,
            publishableKey = requireNotNull(response.publishableKey),
            name = requireNotNull(response.name),
            scopes = response.scopes ?: emptyList(),
            domains = response.domains ?: emptyList(),
            status = requireNotNull(response.status),
            expiration = requireNotNull(response.expiration),
            metadata = response.metadata ?: emptyMap(),
            lastUsedAt = response.lastUsedAt,
            createdAt = requireNotNull(response.createdAt)
        )
    }

    fun create(params: CreateApiKeyParams): CreateApiKeyResult {
        return httpClient.post<CreateApiKeyResult, CreateApiKeyRequestWire>(
            path = "/api/api-keys",
            body = CreateApiKeyRequestWire(
                name = params.name,
                scopes = params.scopes,
                domains = params.domains,
                expiration = params.expiration,
                metadata = params.metadata
            )
        )
    }

    fun exchange(params: ExchangeApiKeyParams): ExchangeApiKeyResult {
        return httpClient.post<ExchangeApiKeyResult, ExchangeApiKeyRequestWire>(
            path = "/api/api-keys/exchange",
            body = ExchangeApiKeyRequestWire(
                key = params.key,
                scopes = params.scopes
            )
        )
    }

    fun revoke(id: String) {
        httpClient.delete<MessageResponseWire>(path = "/api/api-keys/$id")
    }

    fun rotate(id: String): RotateApiKeyResult {
        return httpClient.post<RotateApiKeyResult, Map<String, String>>(
            path = "/api/api-keys/$id/rotate",
            body = emptyMap()
        )
    }
}
