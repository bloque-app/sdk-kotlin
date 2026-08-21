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

    /**
     * Create an opaque API key pair. The secret is shown only once.
     *
     * When the session holds a `kind: origin-operator` JWT (after
     * `orgs.assumeOrigin`), the server binds the key to that origin
     * (`bound_origin`) and owns it as the controller org. Scopes are
     * capped to `OriginOperatorRoleContext` (read-only origin-scoped;
     * no `*.any`, pay/create, or passkey-as-user). A normal user JWT
     * still mints an unbound personal key.
     */
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

    /**
     * Exchange an `sk_` secret for a short-lived JWT.
     *
     * Unbound keys need no Authorization. Origin-bound keys with
     * [ExchangeApiKeyParams.asIdentity] require the session's Bearer to
     * be `kind: origin-operator` (set by `orgs.assumeOrigin`).
     */
    fun exchange(params: ExchangeApiKeyParams): ExchangeApiKeyResult {
        return httpClient.post<ExchangeApiKeyResult, ExchangeApiKeyRequestWire>(
            path = "/api/api-keys/exchange",
            body = ExchangeApiKeyRequestWire(
                key = params.key,
                scopes = params.scopes,
                asIdentity = params.asIdentity
            )
        )
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
