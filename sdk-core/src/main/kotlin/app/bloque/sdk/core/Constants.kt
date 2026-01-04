package app.bloque.sdk.core

/**
 * API base URLs for different environments
 */
object ApiBaseUrls {
    const val SANDBOX = "https://dev.bloque.app"
    const val PRODUCTION = "https://api.bloque.app"
}

/**
 * Default HTTP headers
 */
object DefaultHeaders {
    const val CONTENT_TYPE = "application/json"
    const val ACCEPT = "application/json"
}

/**
 * HTTP timeouts in seconds
 */
object Timeouts {
    const val CONNECT = 30L
    const val READ = 30L
    const val WRITE = 30L
}

/**
 * Retry configuration defaults
 */
object RetryDefaults {
    const val ENABLED = true
    const val MAX_RETRIES = 3
    const val INITIAL_DELAY_MS = 1000L
    const val MAX_DELAY_MS = 10000L
    const val BACKOFF_MULTIPLIER = 2.0
}
