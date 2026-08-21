package app.bloque.sdk

import app.bloque.sdk.core.AuthConfig
import app.bloque.sdk.core.BloqueConfig
import app.bloque.sdk.core.BloqueConfigError
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.Mode
import app.bloque.sdk.core.RetryConfig
import app.bloque.sdk.identity.IdentityClient
import app.bloque.sdk.identity.OriginsClient
import app.bloque.sdk.identity.RegisterParams

/**
 * Main entry point for Bloque SDK.
 *
 * Two authentication strategies:
 *
 * **ApiKey (recommended)** — uses sk_ secret keys that are auto-exchanged for JWTs:
 * ```kotlin
 * val bloque = BloqueSDK.builder()
 *     .secretKey("sk_live_...")
 *     .mode(Mode.PRODUCTION)
 *     .build()
 *
 * val session = bloque.connect()  // no alias needed
 * ```
 *
 * **OriginKey (legacy)** — origin-scoped keys requiring alias:
 * ```kotlin
 * val bloque = BloqueSDK.builder()
 *     .origin("my-origin")
 *     .originKey("my-origin-key")
 *     .mode(Mode.PRODUCTION)
 *     .build()
 *
 * val session = bloque.connect("@alice")
 * ```
 */
class BloqueSDK private constructor(
    private val config: BloqueConfig
) {
    private val httpClient = BloqueHttpClient(config)

    /**
     * Self-service origin configuration, authenticated purely by the
     * origin's own key — no connected session required. Sibling to
     * [connect]/[register], but for configuring the origin itself rather
     * than one of its identities.
     */
    val origins: OriginsConfigClient = OriginsConfigClient(httpClient, config)

    /**
     * Register a new identity and get an authenticated session.
     * Only available for OriginKey auth.
     *
     * @param alias The user alias (e.g., username, email, phone number)
     * @param params Registration parameters (individual or business profile)
     * @return UserSession with access to all SDK modules
     * @throws BloqueConfigError if auth is not OriginKey
     */
    fun register(alias: String, params: RegisterParams): UserSession {
        val auth = config.auth
        if (auth !is AuthConfig.OriginKey) {
            throw BloqueConfigError("register() requires OriginKey auth — use originKey() in the builder")
        }

        val originsClient = OriginsClient(httpClient)
        val origin = requireNotNull(config.origin) { "origin is required for OriginKey auth" }
        params.setOriginData(alias, origin)

        val result = originsClient.register(params, auth.originKey)

        httpClient.updateAccessToken(result.accessToken)
        httpClient.updateUrn(result.urn)

        return UserSession(httpClient)
    }

    /**
     * Connect using ApiKey auth (sk_ secret key).
     * The SDK auto-exchanges the key for a JWT and resolves the identity via /me.
     *
     * @return UserSession with access to all SDK modules
     * @throws BloqueConfigError if auth is not ApiKey
     */
    fun connect(): UserSession {
        val auth = config.auth
        if (auth !is AuthConfig.ApiKey) {
            throw BloqueConfigError("connect() without alias requires ApiKey auth — use secretKey() in the builder")
        }

        httpClient.ensureExchanged()

        val identityClient = IdentityClient(httpClient)
        val me = identityClient.me()

        httpClient.setOrigin(me.origin)
        httpClient.updateUrn(me.urn)

        return UserSession(httpClient)
    }

    /**
     * Connect with an alias using OriginKey auth (legacy flow).
     *
     * @param alias The user alias (e.g., "@alice", "user@email.com", "+1234567890")
     * @return UserSession with access to all SDK modules
     * @throws BloqueConfigError if auth is not OriginKey
     */
    fun connect(alias: String): UserSession {
        val auth = config.auth
        if (auth !is AuthConfig.OriginKey) {
            throw BloqueConfigError("connect(alias) requires OriginKey auth — use originKey() in the builder")
        }

        val origin = requireNotNull(config.origin) { "origin is required for OriginKey auth" }
        val urn = "did:bloque:$origin:$alias"

        val request = ConnectRequest(
            assertionResult = AssertionResult(
                challengeType = "API_KEY",
                value = AssertionValue(
                    apiKey = auth.originKey,
                    alias = alias
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

    companion object {
        @JvmStatic
        fun builder() = Builder()

        /**
         * Create SDK with an sk_ secret key (recommended).
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            secretKey: String,
            mode: Mode = Mode.PRODUCTION,
            timeoutMs: Long = 30000,
            retry: RetryConfig = RetryConfig.DEFAULT
        ): BloqueSDK {
            return Builder()
                .secretKey(secretKey)
                .mode(mode)
                .timeout(timeoutMs)
                .retry(retry)
                .build()
        }

        /**
         * Create SDK with a legacy origin key.
         */
        @JvmStatic
        @JvmOverloads
        fun createWithOriginKey(
            origin: String,
            originKey: String,
            mode: Mode = Mode.PRODUCTION,
            timeoutMs: Long = 30000,
            retry: RetryConfig = RetryConfig.DEFAULT
        ): BloqueSDK {
            return Builder()
                .origin(origin)
                .originKey(originKey)
                .mode(mode)
                .timeout(timeoutMs)
                .retry(retry)
                .build()
        }

        @Deprecated("Use create(secretKey) or createWithOriginKey(origin, originKey)", ReplaceWith("createWithOriginKey(origin, apiKey)"))
        @JvmStatic
        @JvmOverloads
        fun create(
            origin: String,
            apiKey: String,
            mode: Mode = Mode.PRODUCTION,
            timeoutMs: Long = 30000,
            retry: RetryConfig = RetryConfig.DEFAULT
        ): BloqueSDK {
            return createWithOriginKey(origin, apiKey, mode, timeoutMs, retry)
        }
    }

    class Builder {
        private val configBuilder = BloqueConfig.builder()

        fun origin(origin: String) = apply { configBuilder.origin(origin) }

        fun secretKey(secretKey: String, scopes: List<String>? = null) = apply {
            configBuilder.secretKey(secretKey, scopes)
        }

        fun originKey(originKey: String) = apply { configBuilder.originKey(originKey) }

        @Deprecated("Use secretKey() for sk_ keys or originKey() for legacy keys", ReplaceWith("secretKey(apiKey)"))
        fun apiKey(apiKey: String) = apply { configBuilder.apiKey(apiKey) }

        fun mode(mode: Mode) = apply { configBuilder.mode(mode) }

        fun timeout(timeoutMs: Long) = apply { configBuilder.timeout(timeoutMs) }

        fun retry(retry: RetryConfig) = apply { configBuilder.retry(retry) }

        fun retry(block: RetryConfig.Builder.() -> Unit) = apply { configBuilder.retry(block) }

        fun baseUrl(baseUrl: String) = apply { configBuilder.baseUrl(baseUrl) }

        fun build(): BloqueSDK {
            return BloqueSDK(configBuilder.build())
        }
    }
}
