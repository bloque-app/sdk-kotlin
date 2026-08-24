package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Main client for identity operations
 */
class IdentityClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    val aliases: AliasesClient = AliasesClient(httpClient)
    val origins: OriginsClient = OriginsClient(httpClient)
    val apiKeys: ApiKeysClient = ApiKeysClient(httpClient)

    private fun mapToJsonElement(map: Map<String, Any?>?): JsonElement? {
        if (map == null) return null
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    null -> put(key, JsonNull)
                    is String -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        put(key, mapToJsonElement(value as Map<String, Any?>) ?: JsonNull)
                    }
                    is List<*> -> put(key, buildJsonArray {
                        value.forEach { item ->
                            when (item) {
                                is String -> add(JsonPrimitive(item))
                                is Number -> add(JsonPrimitive(item))
                                is Boolean -> add(JsonPrimitive(item))
                                is Map<*, *> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    add(mapToJsonElement(item as Map<String, Any?>) ?: JsonNull)
                                }
                                else -> add(if (item == null) JsonNull else JsonPrimitive(item.toString()))
                            }
                        }
                    })
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }
    }

    /**
     * Retrieve the authenticated identity's own profile.
     * Used by ApiKey auth to discover origin and urn after exchange.
     */
    fun me(): IdentityMe {
        return httpClient.get<IdentityMe>(path = "/api/identities/me")
    }

    /**
     * Fetch any accessible identity by URN. Access is subject to policy
     * evaluation.
     *
     * @param urn The URN of the identity to retrieve
     */
    fun get(urn: String): IdentityMe {
        return httpClient.get<IdentityMe>(path = "/api/identities/$urn")
    }

    /**
     * Update the authenticated identity's own `profile`/`metadata`. Each is
     * shallow-merged server-side into the existing values.
     */
    fun updateMe(params: UpdateIdentityParams): IdentityMe {
        val response = httpClient.patch<UpdateIdentityResponseWire, UpdateIdentityRequestWire>(
            path = "/api/identities/me",
            body = UpdateIdentityRequestWire(
                profile = params.profile,
                metadata = mapToJsonElement(params.metadata)
            )
        )
        return response.result.identity
    }

    /**
     * Update another identity's `profile`/`metadata` by URN. Access is
     * subject to policy evaluation. Each field is shallow-merged
     * server-side into the existing values.
     *
     * @param urn The URN of the identity to update
     */
    fun update(urn: String, params: UpdateIdentityParams): IdentityMe {
        val response = httpClient.patch<UpdateIdentityResponseWire, UpdateIdentityRequestWire>(
            path = "/api/identities/$urn",
            body = UpdateIdentityRequestWire(
                profile = params.profile,
                metadata = mapToJsonElement(params.metadata)
            )
        )
        return response.result.identity
    }
}
