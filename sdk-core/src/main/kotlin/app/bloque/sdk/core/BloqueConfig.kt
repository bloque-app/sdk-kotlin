package app.bloque.sdk.core

/**
 * Authentication configuration for the SDK.
 *
 * - [ApiKey]: Uses sk_ secret keys that are auto-exchanged for short-lived JWTs.
 *   Origin is resolved at runtime via /me. Recommended for new integrations.
 * - [OriginKey]: Legacy origin-scoped keys. Requires origin + alias for connect().
 */
sealed class AuthConfig {
    data class ApiKey(val secretKey: String, val scopes: List<String>? = null) : AuthConfig()
    data class OriginKey(val originKey: String) : AuthConfig()
}

/**
 * SDK environment mode
 */
enum class Mode(val baseUrl: String) {
    PRODUCTION("https://api.bloque.app"),
    SANDBOX("https://dev.bloque.app")
}

/**
 * Retry configuration for API requests
 *
 * @param maxRetries Maximum number of retry attempts (default: 3)
 * @param initialDelayMs Initial delay between retries in milliseconds (default: 1000)
 * @param maxDelayMs Maximum delay between retries in milliseconds (default: 10000)
 * @param backoffMultiplier Multiplier for exponential backoff (default: 2.0)
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        require(initialDelayMs > 0) { "initialDelayMs must be > 0" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
    }

    companion object {
        /**
         * No retry configuration
         */
        @JvmStatic
        val NONE = RetryConfig(maxRetries = 0)

        /**
         * Default retry configuration
         */
        @JvmStatic
        val DEFAULT = RetryConfig()

        /**
         * Create a builder for RetryConfig (recommended for Java)
         */
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var maxRetries: Int = 3
        private var initialDelayMs: Long = 1000
        private var maxDelayMs: Long = 10000
        private var backoffMultiplier: Double = 2.0

        fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }
        fun initialDelayMs(initialDelayMs: Long) = apply { this.initialDelayMs = initialDelayMs }
        fun initialDelay(initialDelayMs: Long) = apply { this.initialDelayMs = initialDelayMs }
        fun maxDelayMs(maxDelayMs: Long) = apply { this.maxDelayMs = maxDelayMs }
        fun backoffMultiplier(backoffMultiplier: Double) = apply { this.backoffMultiplier = backoffMultiplier }

        fun build(): RetryConfig = RetryConfig(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            backoffMultiplier = backoffMultiplier
        )
    }
}

/**
 * Configuration for Bloque SDK
 *
 * @param origin The origin identifier. Required for OriginKey auth, resolved at runtime for ApiKey auth.
 * @param auth Authentication configuration
 * @param mode SDK environment mode (PRODUCTION or SANDBOX)
 * @param timeoutMs Request timeout in milliseconds (default: 30000)
 * @param retry Retry configuration for failed requests
 */
data class BloqueConfig(
    val origin: String?,
    val auth: AuthConfig,
    val mode: Mode = Mode.PRODUCTION,
    val timeoutMs: Long = 30000,
    val retry: RetryConfig = RetryConfig.DEFAULT
) {
    val baseUrl: String get() = mode.baseUrl

    val originKey: String
        get() = when (auth) {
            is AuthConfig.OriginKey -> auth.originKey
            is AuthConfig.ApiKey -> throw BloqueConfigError("originKey is not available for ApiKey auth")
        }

    class Builder {
        private var origin: String? = null
        private var auth: AuthConfig? = null
        private var mode: Mode = Mode.PRODUCTION
        private var timeoutMs: Long = 30000
        private var retry: RetryConfig = RetryConfig.DEFAULT

        fun origin(origin: String) = apply { this.origin = origin }

        /**
         * Configure authentication with an sk_ secret key (recommended).
         * The SDK will auto-exchange this key for short-lived JWTs.
         * Origin is not required — it will be resolved via /me at connect time.
         */
        fun secretKey(secretKey: String, scopes: List<String>? = null) = apply {
            this.auth = AuthConfig.ApiKey(secretKey, scopes)
        }

        /**
         * Configure authentication with a legacy origin-scoped key.
         * Requires origin to be set. Use connect(alias) to authenticate.
         */
        fun originKey(originKey: String) = apply { this.auth = AuthConfig.OriginKey(originKey) }

        @Deprecated("Use secretKey() for sk_ keys or originKey() for legacy keys", ReplaceWith("secretKey(apiKey)"))
        fun apiKey(apiKey: String) = apply { this.auth = AuthConfig.ApiKey(apiKey) }

        fun mode(mode: Mode) = apply { this.mode = mode }
        fun timeout(timeoutMs: Long) = apply { this.timeoutMs = timeoutMs }
        fun timeoutMs(timeoutMs: Long) = apply { this.timeoutMs = timeoutMs }
        fun retry(retry: RetryConfig) = apply { this.retry = retry }
        fun retry(block: RetryConfig.Builder.() -> Unit) = apply {
            this.retry = RetryConfig.Builder().apply(block).build()
        }

        fun build(): BloqueConfig {
            val resolvedAuth = requireNotNull(auth) { "auth is required — call secretKey() or originKey()" }
            if (resolvedAuth is AuthConfig.OriginKey) {
                requireNotNull(origin) { "origin is required for originKey auth" }
            }
            return BloqueConfig(
                origin = origin,
                auth = resolvedAuth,
                mode = mode,
                timeoutMs = timeoutMs,
                retry = retry
            )
        }
    }

    companion object {
        fun builder() = Builder()
    }
}
