package app.bloque.sdk.core

/**
 * Authentication configuration for the SDK
 */
sealed class AuthConfig {
    data class ApiKey(val apiKey: String) : AuthConfig()
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
 * @param origin The origin identifier for authentication
 * @param auth Authentication configuration
 * @param mode SDK environment mode (PRODUCTION or SANDBOX)
 * @param timeoutMs Request timeout in milliseconds (default: 30000)
 * @param retry Retry configuration for failed requests
 */
data class BloqueConfig(
    val origin: String,
    val auth: AuthConfig,
    val mode: Mode = Mode.PRODUCTION,
    val timeoutMs: Long = 30000,
    val retry: RetryConfig = RetryConfig.DEFAULT
) {
    val baseUrl: String get() = mode.baseUrl

    val apiKey: String
        get() = when (auth) {
            is AuthConfig.ApiKey -> auth.apiKey
        }

    class Builder {
        private var origin: String? = null
        private var auth: AuthConfig? = null
        private var mode: Mode = Mode.PRODUCTION
        private var timeoutMs: Long = 30000
        private var retry: RetryConfig = RetryConfig.DEFAULT

        fun origin(origin: String) = apply { this.origin = origin }
        fun apiKey(apiKey: String) = apply { this.auth = AuthConfig.ApiKey(apiKey) }
        fun mode(mode: Mode) = apply { this.mode = mode }
        fun timeout(timeoutMs: Long) = apply { this.timeoutMs = timeoutMs }
        fun timeoutMs(timeoutMs: Long) = apply { this.timeoutMs = timeoutMs }
        fun retry(retry: RetryConfig) = apply { this.retry = retry }
        fun retry(block: RetryConfig.Builder.() -> Unit) = apply {
            this.retry = RetryConfig.Builder().apply(block).build()
        }

        fun build(): BloqueConfig {
            return BloqueConfig(
                origin = requireNotNull(origin) { "origin is required" },
                auth = requireNotNull(auth) { "auth is required" },
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
