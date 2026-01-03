package app.bloque.sdk.core

/**
 * Base exception for all SDK errors
 */
open class BloqueException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when API returns an error response
 */
class BloqueApiException(
    val statusCode: Int,
    val errorBody: String?,
    message: String = "API error: $statusCode"
) : BloqueException(message)

/**
 * Exception thrown when network fails
 */
class BloqueNetworkException(
    message: String,
    cause: Throwable? = null
) : BloqueException(message, cause)

/**
 * Exception thrown when serialization/deserialization fails
 */
class BloqueSerializationException(
    message: String,
    cause: Throwable? = null
) : BloqueException(message, cause)
