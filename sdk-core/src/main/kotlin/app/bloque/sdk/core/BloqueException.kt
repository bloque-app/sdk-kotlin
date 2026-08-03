package app.bloque.sdk.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

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
open class BloqueRateLimitError(
    statusCode: Int = 429,
    errorBody: String?,
    message: String = "Rate limit exceeded"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Exception thrown when the compliance engine blocks an action because it
 * would exceed the caller's tier limit for a given window
 * (`E_TIER_LIMIT_EXCEEDED`, HTTP 429) — distinct from the generic
 * [BloqueRateLimitError], which is about request throughput, not
 * money-movement volume.
 */
class BloqueTierLimitExceededError(
    statusCode: Int,
    errorBody: String?,
    message: String,
    /** The limit window that was exceeded (`per_transaction`, `day`, `week`, `month`, or `year`). */
    val window: String = "unknown",
    /** The specific window key that was exceeded (e.g. a calendar day/week/month/year key), when available. */
    val windowKey: String? = null,
    /** ISO 8601 timestamp when this window resets, when available. */
    val resetAt: String? = null,
    /** The window's limit, in USD minor units (cents), as a decimal string. */
    val limitUsdMinorUnits: String? = null,
    /** USD minor units already consumed in this window, as a decimal string, when available. */
    val consumedUsdMinorUnits: String? = null
) : BloqueRateLimitError(statusCode, errorBody, message)

/**
 * Exception thrown for authentication errors (401/403)
 */
open class BloqueAuthenticationError(
    statusCode: Int,
    errorBody: String?,
    message: String = "Authentication failed"
) : BloqueAPIError(statusCode, errorBody, message)

/**
 * Result of [BloqueVerificationRequiredError.getVerificationLink] — a portable
 * hosted-page URL your user can open to resolve the outstanding requirement.
 */
data class VerificationLinkResult(
    val url: String,
    val expiresIn: String
)

/**
 * Exception thrown when the compliance engine blocks an action because the
 * caller's identity has not met the minimum verification tier
 * (`E_VERIFICATION_REQUIRED`, HTTP 403).
 *
 * [reason] tells you what kind of verification is outstanding, and
 * [getVerificationLink] starts the matching hosted gate flow (TOS gate or
 * verification gate) so you don't need to hardcode either endpoint — it
 * reads the `start_endpoint` the compliance engine returned.
 */
class BloqueVerificationRequiredError(
    statusCode: Int,
    errorBody: String?,
    message: String,
    /** What kind of verification is outstanding. `"kyc"` has no hosted-page
     * handoff — [getVerificationLink] returns `null` for it. */
    val reason: String,
    /** The caller's current effective tier level. */
    val currentLevel: Int? = null,
    /** The minimum tier level required for the attempted action. */
    val requiredLevel: Int? = null,
    /** Requirement keys still outstanding at the caller's next tier level. */
    val missingRequirements: List<String> = emptyList(),
    /**
     * The subset of [missingRequirements] your user has already submitted
     * and that is waiting on a reviewer. Do not ask for these again — the
     * rest of [missingRequirements] is what is actually actionable.
     */
    val pendingRequirements: List<String> = emptyList(),
    private val startEndpoint: String? = null,
    private val httpClient: BloqueHttpClient? = null
) : BloqueAuthenticationError(statusCode, errorBody, message) {

    /**
     * Starts the hosted gate flow this error points to (TOS gate or
     * verification gate) and returns the URL your user should open.
     *
     * Returns `null` when there is no hosted-page handoff for this gap
     * ([reason] is `"kyc"`, or the response didn't include one).
     */
    fun getVerificationLink(returnUrl: String): VerificationLinkResult? {
        val endpoint = startEndpoint ?: return null
        val client = httpClient ?: return null
        val response = client.post<GateStartLinkResponseWire, GateStartLinkRequestWire>(
            path = endpoint,
            body = GateStartLinkRequestWire(returnUrl = returnUrl)
        )
        return VerificationLinkResult(url = response.url, expiresIn = response.expiresIn)
    }
}

/**
 * Exception thrown when the compliance engine blocks an action but your user
 * has already submitted everything it is waiting on
 * (`E_VERIFICATION_PENDING`, HTTP 403).
 *
 * The distinction from [BloqueVerificationRequiredError] matters in your UI:
 * there is deliberately no `getVerificationLink()` here, because opening a
 * gate would ask your user to re-send documents a reviewer is already
 * holding. Show them that the review is in progress and retry the original
 * action later — it succeeds once the review lands.
 */
class BloqueVerificationPendingError(
    statusCode: Int,
    errorBody: String?,
    message: String,
    /** The caller's current effective tier level. */
    val currentLevel: Int? = null,
    /** The minimum tier level required for the attempted action. */
    val requiredLevel: Int? = null,
    /** Requirement keys submitted and awaiting review. */
    val pendingRequirements: List<String> = emptyList()
) : BloqueAuthenticationError(statusCode, errorBody, message)

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

// ============================================
// Error body parsing (BError wire shape: { error, code, message, extra_details })
// ============================================

@PublishedApi
internal val errorJson: Json = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
internal data class ApiErrorBodyWire(
    val code: String? = null,
    val message: String? = null,
    @SerialName("extra_details") val extraDetails: JsonElement? = null
)

@Serializable
internal data class VerificationFlowHandoffErrorWire(
    val type: String? = null,
    val method: String? = null,
    @SerialName("start_endpoint") val startEndpoint: String? = null
)

@Serializable
internal data class VerificationRequiredExtraDetailsWire(
    @SerialName("current_level") val currentLevel: Int? = null,
    @SerialName("required_level") val requiredLevel: Int? = null,
    @SerialName("missing_requirements") val missingRequirements: List<String>? = null,
    @SerialName("pending_requirements") val pendingRequirements: List<String>? = null,
    @SerialName("verification_flow") val verificationFlow: VerificationFlowHandoffErrorWire? = null
)

@Serializable
internal data class TierLimitExtraDetailsWire(
    val window: String? = null,
    @SerialName("window_key") val windowKey: String? = null,
    @SerialName("reset_at") val resetAt: String? = null,
    @SerialName("limit_usd_minor_units") val limitUsdMinorUnits: String? = null,
    @SerialName("consumed_usd_minor_units") val consumedUsdMinorUnits: String? = null
)

@Serializable
internal data class GateStartLinkRequestWire(@SerialName("return_url") val returnUrl: String)

@Serializable
internal data class GateStartLinkResponseWire(
    val token: String,
    val url: String,
    @SerialName("expires_in") val expiresIn: String
)

private fun parseApiErrorBody(errorBody: String?): ApiErrorBodyWire? {
    if (errorBody.isNullOrBlank()) return null
    return try {
        errorJson.decodeFromString(ApiErrorBodyWire.serializer(), errorBody)
    } catch (e: Exception) {
        null
    }
}

private inline fun <reified T> decodeExtraDetails(element: JsonElement?): T? {
    if (element == null) return null
    return try {
        errorJson.decodeFromJsonElement<T>(element)
    } catch (e: Exception) {
        null
    }
}

private fun verificationReason(
    flow: VerificationFlowHandoffErrorWire?,
    missingRequirements: List<String>
): String = when {
    flow?.type == "tos_hosted_acceptance" -> "tos"
    flow?.type == "document_submission" -> "documents"
    missingRequirements.isNotEmpty() -> "kyc"
    else -> "unknown"
}

private fun verificationRequiredMessage(reason: String, missingRequirements: List<String>): String =
    when (reason) {
        "tos" -> "Please accept the Terms of Service before continuing. Call getVerificationLink() to get a link your user can open."
        "documents" -> "Additional information or documents are required before continuing. Call getVerificationLink() to get a link your user can open."
        "kyc" -> "Identity verification (KYC) is required before continuing."
        else -> if (missingRequirements.isNotEmpty()) {
            "Verification required: ${missingRequirements.joinToString(", ")}."
        } else {
            "Verification required before continuing."
        }
    }

private fun tierLimitMessage(extra: TierLimitExtraDetailsWire?): String {
    val window = extra?.window ?: return "You've reached a usage limit."
    val limit = extra.limitUsdMinorUnits?.let { " of $${"%.2f".format(it.toDouble() / 100)}" } ?: ""
    val resetHint = extra.resetAt?.let { " Try again after $it." } ?: ""
    return "You've reached your $window limit$limit.$resetHint"
}

/**
 * Factory function to create appropriate error based on status code and,
 * where present, the `code`/`extra_details` on the parsed API error body.
 */
@PublishedApi
internal fun createBloqueError(
    statusCode: Int,
    errorBody: String?,
    defaultMessage: String,
    httpClient: BloqueHttpClient? = null
): BloqueAPIError {
    val parsed = parseApiErrorBody(errorBody)

    when (parsed?.code) {
        "E_VERIFICATION_REQUIRED" -> {
            val extra = decodeExtraDetails<VerificationRequiredExtraDetailsWire>(parsed.extraDetails)
            val missing = extra?.missingRequirements ?: emptyList()
            val reason = verificationReason(extra?.verificationFlow, missing)
            return BloqueVerificationRequiredError(
                statusCode = statusCode,
                errorBody = errorBody,
                message = verificationRequiredMessage(reason, missing),
                reason = reason,
                currentLevel = extra?.currentLevel,
                requiredLevel = extra?.requiredLevel,
                missingRequirements = missing,
                pendingRequirements = extra?.pendingRequirements ?: emptyList(),
                startEndpoint = extra?.verificationFlow?.startEndpoint,
                httpClient = httpClient
            )
        }

        "E_VERIFICATION_PENDING" -> {
            val extra = decodeExtraDetails<VerificationRequiredExtraDetailsWire>(parsed.extraDetails)
            return BloqueVerificationPendingError(
                statusCode = statusCode,
                errorBody = errorBody,
                message = "Your submission is being reviewed. No further action is needed right now — retry once the review is complete.",
                currentLevel = extra?.currentLevel,
                requiredLevel = extra?.requiredLevel,
                pendingRequirements = extra?.pendingRequirements ?: emptyList()
            )
        }

        "E_TIER_LIMIT_EXCEEDED" -> {
            val extra = decodeExtraDetails<TierLimitExtraDetailsWire>(parsed.extraDetails)
            return BloqueTierLimitExceededError(
                statusCode = statusCode,
                errorBody = errorBody,
                message = tierLimitMessage(extra),
                window = extra?.window ?: "unknown",
                windowKey = extra?.windowKey,
                resetAt = extra?.resetAt,
                limitUsdMinorUnits = extra?.limitUsdMinorUnits,
                consumedUsdMinorUnits = extra?.consumedUsdMinorUnits
            )
        }
    }

    return when (statusCode) {
        429 -> BloqueRateLimitError(statusCode, errorBody, defaultMessage)
        401, 403 -> BloqueAuthenticationError(statusCode, errorBody, defaultMessage)
        400 -> BloqueValidationError(statusCode, errorBody, defaultMessage)
        404 -> BloqueNotFoundError(statusCode, errorBody, defaultMessage)
        else -> BloqueApiException(statusCode, errorBody, defaultMessage)
    }
}
