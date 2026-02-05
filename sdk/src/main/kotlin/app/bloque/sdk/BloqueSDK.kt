package app.bloque.sdk

import app.bloque.sdk.core.AuthConfig
import app.bloque.sdk.core.BloqueConfig
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.Mode
import app.bloque.sdk.core.RetryConfig
import app.bloque.sdk.identity.BusinessRegisterParams
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.OriginsClient
import app.bloque.sdk.identity.RegisterParams

/**
 * Main entry point for Bloque SDK
 *
 * Usage (Kotlin):
 * ```kotlin
 * val bloque = BloqueSDK.builder()
 *     .origin("my-origin")
 *     .apiKey("sk_live_...")
 *     .mode(Mode.PRODUCTION)
 *     .timeout(10000)
 *     .retry {
 *         maxRetries(3)
 *         initialDelay(1000)
 *     }
 *     .build()
 *
 * val session = bloque.connect("nestor")
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
 *     .timeout(10000)
 *     .retry(RetryConfig.builder()
 *         .maxRetries(3)
 *         .initialDelay(1000)
 *         .build())
 *     .build();
 *
 * UserSession session = bloque.connect("nestor");
 * BancolombiaAccount account = session.getAccounts().getBancolombia().create();
 * ```
 */
class BloqueSDK private constructor(
    private val config: BloqueConfig
) {
    private val httpClient = BloqueHttpClient(config)

    /**
     * Register a new identity and get an authenticated session
     *
     * @param alias The user alias (e.g., username, email, phone number)
     * @param params Registration parameters (individual or business profile)
     * @return UserSession with access to all SDK modules
     */
    fun register(alias: String, params: RegisterParams): UserSession {
        val originsClient = OriginsClient(httpClient)

        // Set alias and origin from SDK config
        params.setOriginData(alias, config.origin)

        val result = originsClient.register(params, config.apiKey)

        httpClient.updateAccessToken(result.accessToken)
        httpClient.updateUrn(result.urn)

        return UserSession(httpClient)
    }

    /**
     * Connect with an alias and get an authenticated session
     *
     * @param alias The user alias (e.g., "nestor", "user@email.com", "+1234567890")
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
                    alias = alias  // Use original alias, not URN
                )
            ),
            extraContext = emptyMap()
        )

        val response = httpClient.post<ConnectResponse, ConnectRequest>(
            path = "/api/origins/$origin/connect",
            body = request
        )

        httpClient.updateAccessToken(response.result.accessToken)
        httpClient.updateUrn(urn)  // Set URN after successful authentication

        return UserSession(httpClient)
    }

    private fun buildUrn(alias: String): String {
        return "did:bloque:${config.origin}:$alias"
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
            mode: Mode = Mode.PRODUCTION,
            timeoutMs: Long = 30000,
            retry: RetryConfig = RetryConfig.DEFAULT
        ): BloqueSDK {
            return Builder()
                .origin(origin)
                .apiKey(apiKey)
                .mode(mode)
                .timeout(timeoutMs)
                .retry(retry)
                .build()
        }
    }

    class Builder {
        private var origin: String? = null
        private var apiKey: String? = null
        private var mode: Mode = Mode.PRODUCTION
        private var timeoutMs: Long = 30000
        private var retry: RetryConfig = RetryConfig.DEFAULT

        fun origin(origin: String) = apply { this.origin = origin }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun mode(mode: Mode) = apply { this.mode = mode }

        /**
         * Set request timeout in milliseconds
         * @param timeoutMs Timeout in milliseconds (default: 30000)
         */
        fun timeout(timeoutMs: Long) = apply { this.timeoutMs = timeoutMs }

        /**
         * Set retry configuration
         * @param retry RetryConfig instance
         */
        fun retry(retry: RetryConfig) = apply { this.retry = retry }

        /**
         * Set retry configuration using a builder block (Kotlin DSL)
         * @param block Configuration block for RetryConfig.Builder
         */
        fun retry(block: RetryConfig.Builder.() -> Unit) = apply {
            this.retry = RetryConfig.Builder().apply(block).build()
        }

        fun build(): BloqueSDK {
            val config = BloqueConfig(
                origin = requireNotNull(origin) { "origin is required" },
                auth = AuthConfig.ApiKey(requireNotNull(apiKey) { "apiKey is required" }),
                mode = mode,
                timeoutMs = timeoutMs,
                retry = retry
            )
            return BloqueSDK(config)
        }
    }
}
