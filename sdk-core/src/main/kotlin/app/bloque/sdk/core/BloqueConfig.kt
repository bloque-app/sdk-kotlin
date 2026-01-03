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
 * Configuration for Bloque SDK
 */
data class BloqueConfig(
    val origin: String,
    val auth: AuthConfig,
    val mode: Mode = Mode.PRODUCTION
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

        fun origin(origin: String) = apply { this.origin = origin }
        fun apiKey(apiKey: String) = apply { this.auth = AuthConfig.ApiKey(apiKey) }
        fun mode(mode: Mode) = apply { this.mode = mode }

        fun build(): BloqueConfig {
            return BloqueConfig(
                origin = requireNotNull(origin) { "origin is required" },
                auth = requireNotNull(auth) { "auth is required" },
                mode = mode
            )
        }
    }

    companion object {
        fun builder() = Builder()
    }
}
