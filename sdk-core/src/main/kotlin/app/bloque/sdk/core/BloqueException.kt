package app.bloque.sdk.core

/**
 * Base exception for all SDK errors
 */
open class BloqueException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Base exception for API-related errors
 */
open class BloqueAPIError(
    val statusCode: Int,
    val errorBody: String?,
    message: String
) : BloqueException(message)

/**
 * Exception thrown when API returns an error response
 */
class BloqueApiException(
    statusCode: Int,
    errorBody: String?,
    message: String = "API error: $statusCode"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown for rate limiting (429)
 */
class BloqueRateLimitError(
    statusCode: Int = 429,
    errorBody: String?,
    message: String = "Rate limit exceeded"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown for authentication errors (401/403)
 */
class BloqueAuthenticationError(
    statusCode: Int,
    errorBody: String?,
    message: String = "Authentication failed"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown for validation errors (400)
 */
class BloqueValidationError(
    statusCode: Int = 400,
    errorBody: String?,
    message: String = "Validation failed"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown for not found errors (404)
 */
class BloqueNotFoundError(
    statusCode: Int = 404,
    errorBody: String?,
    message: String = "Resource not found"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown for insufficient funds errors
 */
class BloqueInsufficientFundsError(
    statusCode: Int,
    errorBody: String?,
    message: String = "Insufficient funds"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown when network fails
 */
class BloqueNetworkError(
    message: String,
    cause: Throwable? = null
) : BloqueException(message, cause)

/**
 * Exception thrown when request times out
 */
class BloqueTimeoutError(
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

/**
 * Exception thrown for configuration errors
 */
class BloqueConfigError(
    message: String,
    cause: Throwable? = null
) : BloqueException(message, cause)

/**
 * Factory function to create appropriate error based on status code
 */
@PublishedApi
internal fun createBloqueError(statusCode: Int, errorBody: String?, defaultMessage: String): BloqueAPIError {
    return when (statusCode) {
        429 -> BloqueRateLimitError(statusCode, errorBody, defaultMessage)
        401, 403 -> BloqueAuthenticationError(statusCode, errorBody, defaultMessage)
        400 -> BloqueValidationError(statusCode, errorBody, defaultMessage)
        404 -> BloqueNotFoundError(statusCode, errorBody, defaultMessage)
        else -> BloqueApiException(statusCode, errorBody, defaultMessage)
    }
}
