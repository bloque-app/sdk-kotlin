package app.bloque.sdk

import app.bloque.sdk.core.AuthConfig
import app.bloque.sdk.core.BloqueConfig
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Parameters for [OriginsConfigClient.updateMetadata] — a shallow-merge
 * PATCH into an origin's `metadata`, authenticated purely by the origin's
 * own [apiKey] (no session/JWT required, matching how the hosted TOS/
 * verification gates are configured). Only an explicit, extensible
 * allowlist of keys can be set this way; unset fields here are left
 * untouched on the origin (they are not cleared).
 *
 * [originName]/[apiKey] default to the SDK instance's own config
 * (whichever origin/key it was built with) — pass them explicitly only to
 * target a different origin than the one you're authenticated as.
 *
 * These five keys are exactly the ones the hosted gates read at
 * `/tos-gate/start` and `/verification-gate/start` time
 * (`SELF_SERVICE_METADATA_KEYS` in the origins service):
 * - [company] — display name substituted for `{{developer_name}}` in the
 *   TOS document template.
 * - [tosGateShowHome] — whether the TOS gate's intro screens play before
 *   the document (default `true`).
 * - [gateAccentColor] — brand accent color (strict 3-/6-digit CSS hex,
 *   e.g. `"#1a73e8"`) applied to both hosted gates.
 * - [verificationGateReturnUrlAllowlist] — additional `return_url` values
 *   the verification gate will accept for this origin, unioned with the
 *   deployment-wide allowlist.
 * - [gateFrameAncestorsAllowlist] — additional `frame-ancestors` CSP
 *   sources the hosted gates will allow when embedded in an iframe, unioned
 *   with the deployment-wide allowlist.
 */
data class UpdateOriginMetadataParams @JvmOverloads constructor(
    val originName: String? = null,
    val apiKey: String? = null,
    val company: String? = null,
    val tosGateShowHome: Boolean? = null,
    val gateAccentColor: String? = null,
    val verificationGateReturnUrlAllowlist: List<String>? = null,
    val gateFrameAncestorsAllowlist: List<String>? = null
)

data class UpdateOriginMetadataResult(
    val originName: String,
    val updated: Boolean
)

@Serializable
internal data class UpdateOriginMetadataRequestWire(
    @SerialName("api_key") val apiKey: String,
    val metadata: kotlinx.serialization.json.JsonObject
)

@Serializable
internal data class UpdateOriginMetadataResponseWire(
    @SerialName("origin_name") val originName: String,
    val updated: Boolean
)

/**
 * Self-service origin configuration, authenticated purely by the origin's
 * own key — no connected session required. Sibling to [BloqueSDK.connect]/
 * [BloqueSDK.register], but for configuring the origin itself rather than
 * one of its identities.
 */
class OriginsConfigClient internal constructor(
    private val httpClient: BloqueHttpClient,
    private val config: BloqueConfig
) {
    /**
     * Patch this origin's own presentation metadata (`company`,
     * `tosGateShowHome`, `gateAccentColor`,
     * `verificationGateReturnUrlAllowlist`, `gateFrameAncestorsAllowlist`).
     * `originName`/`apiKey` default
     * to this SDK instance's own config, so with secret-key/origin-key
     * auth you typically only set the metadata fields:
     *
     * ```kotlin
     * bloque.origins.updateMetadata(
     *     UpdateOriginMetadataParams(gateAccentColor = "#1a73e8")
     * )
     * ```
     *
     * This is a one-time/deploy-script call, not something you run
     * per-request or per-user. Only the fields you set are patched —
     * omitted fields are left untouched on the origin, they are not
     * cleared.
     */
    fun updateMetadata(params: UpdateOriginMetadataParams): UpdateOriginMetadataResult {
        val originName = params.originName ?: config.origin
            ?: throw BloqueConfigError(
                "origins.updateMetadata() requires originName (or configure the SDK with an origin)"
            )
        val apiKey = params.apiKey ?: resolveOwnApiKey()
            ?: throw BloqueConfigError(
                "origins.updateMetadata() requires apiKey (or configure the SDK with secretKey/originKey auth)"
            )

        val metadata = buildJsonObject {
            params.company?.let { put("company", it) }
            params.tosGateShowHome?.let { put("tos_gate_show_home", it) }
            params.gateAccentColor?.let { put("gate_accent_color", it) }
            params.verificationGateReturnUrlAllowlist?.let { list ->
                put("verification_gate_return_url_allowlist", JsonArray(list.map { JsonPrimitive(it) }))
            }
            params.gateFrameAncestorsAllowlist?.let { list ->
                put("gate_frame_ancestors_allowlist", JsonArray(list.map { JsonPrimitive(it) }))
            }
        }

        val response = httpClient.patch<UpdateOriginMetadataResponseWire, UpdateOriginMetadataRequestWire>(
            path = "/api/origins/$originName/metadata",
            body = UpdateOriginMetadataRequestWire(apiKey = apiKey, metadata = metadata)
        )
        return UpdateOriginMetadataResult(originName = response.originName, updated = response.updated)
    }

    private fun resolveOwnApiKey(): String? = when (val auth = config.auth) {
        is AuthConfig.ApiKey -> auth.secretKey
        is AuthConfig.OriginKey -> auth.originKey
    }
}
