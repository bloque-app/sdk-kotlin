package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for API key management operations. All of these endpoints
 * require an authenticated user session — they cannot be called with
 * only an sk_ key.
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
}
