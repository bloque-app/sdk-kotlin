package app.bloque.sdk

import app.bloque.sdk.core.AuthConfig
import app.bloque.sdk.core.BloqueConfig
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.Mode

/**
 * Main entry point for Bloque SDK
 *
 * Usage (Kotlin):
 * ```kotlin
 * val bloque = BloqueSDK(
 *     origin = "my-origin",
 *     apiKey = "sk_live_...",
 *     mode = Mode.PRODUCTION
 * )
 *
 * val session = bloque.connect("did:bloque:bloque-whatsapp:573023348486")
 * val account = session.accounts.bancolombia.create(
 *     CreateBancolombiaAccountParams(name = "Ahorros")
 * )
 * ```
 *
 * Usage (Java):
 * ```java
 * BloqueSDK bloque = BloqueSDK.builder()
 *     .origin("my-origin")
 *     .apiKey("sk_live_...")
 *     .mode(Mode.PRODUCTION)
 *     .build();
 *
 * UserSession session = bloque.connect("did:bloque:bloque-whatsapp:573023348486");
 * BancolombiaAccount account = session.getAccounts().getBancolombia().create();
 * ```
 */
class BloqueSDK private constructor(
    private val config: BloqueConfig
) {
    private val httpClient = BloqueHttpClient(config)

    /**
     * Connect with an alias/DID and get an authenticated session
     *
     * @param alias The user alias or DID (e.g., "did:bloque:bloque-whatsapp:573023348486")
     * @return UserSession with access to all SDK modules
     */
    fun connect(alias: String): UserSession {
        val urn = buildUrn(alias)
        val origin = httpClient.origin

        val request = ConnectRequest(
            assertionResult = AssertionResult(
                challengeType = "API_KEY",
                value = AssertionValue(
                    apiKey = config.apiKey,
                    alias = urn
                )
            ),
            extraContext = emptyMap()
        )

        val response = httpClient.post<ConnectResponse, ConnectRequest>(
            path = "/api/origins/$origin/connect",
            body = request
        )

        httpClient.updateAccessToken(response.result.accessToken)
        httpClient.updateUrn(urn)

        return UserSession(httpClient)
    }

    private fun buildUrn(alias: String): String {
        return if (alias.startsWith("did:")) {
            alias
        } else {
            "did:bloque:${config.origin}:$alias"
        }
    }

    companion object {
        /**
         * Create SDK with builder pattern (recommended for Java)
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Create SDK directly (convenient for Kotlin)
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            origin: String,
            apiKey: String,
            mode: Mode = Mode.PRODUCTION
        ): BloqueSDK {
            return Builder()
                .origin(origin)
                .apiKey(apiKey)
                .mode(mode)
                .build()
        }
    }

    class Builder {
        private var origin: String? = null
        private var apiKey: String? = null
        private var mode: Mode = Mode.PRODUCTION

        fun origin(origin: String) = apply { this.origin = origin }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun mode(mode: Mode) = apply { this.mode = mode }

        fun build(): BloqueSDK {
            val config = BloqueConfig(
                origin = requireNotNull(origin) { "origin is required" },
                auth = AuthConfig.ApiKey(requireNotNull(apiKey) { "apiKey is required" }),
                mode = mode
            )
            return BloqueSDK(config)
        }
    }
}
