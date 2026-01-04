package app.bloque.sdk.core

/**
 * Base class for all client implementations
 * Provides access to the HTTP client
 */
abstract class BaseClient(
    protected val httpClient: BloqueHttpClient
)
