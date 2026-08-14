package app.bloque.sdk.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val EXCHANGE_REFRESH_BUFFER_MS = 60_000L

@Serializable
internal data class ExchangeRequestWire(
    val key: String,
    val scopes: List<String>? = null
)

@Serializable
internal data class ExchangeResponseWire(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

/**
 * HTTP client for making API requests
 */
class BloqueHttpClient(
    @PublishedApi internal val config: BloqueConfig
) {
    @PublishedApi
    internal var accessToken: String? = null
        private set

    private var urn: String? = null
    private var resolvedOrigin: String? = config.origin

    private var exchangeExpiry: Long = 0

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    @PublishedApi
    internal val retryConfig: RetryConfig = config.retry

    @PublishedApi
    internal val baseUrl: String get() = config.baseUrl

    val auth: AuthConfig get() = config.auth

    val origin: String get() = resolvedOrigin ?: throw BloqueConfigError("origin not yet resolved — call connect() first")

    fun getUrn(): String? = urn

    fun updateAccessToken(token: String) {
        this.accessToken = token
    }

    fun updateUrn(urn: String) {
        this.urn = urn
    }

    fun setOrigin(origin: String) {
        this.resolvedOrigin = origin
    }

    /**
     * Exchange the sk_ secret key for a short-lived JWT.
     * Thread-safe: concurrent callers block on the same synchronized monitor.
     * Auto-refreshes when the cached JWT is within [EXCHANGE_REFRESH_BUFFER_MS] of expiry.
     */
    fun ensureExchanged() {
        if (config.auth !is AuthConfig.ApiKey) return
        val now = System.currentTimeMillis()
        if (accessToken != null && now < exchangeExpiry - EXCHANGE_REFRESH_BUFFER_MS) return

        synchronized(this) {
            val nowInner = System.currentTimeMillis()
            if (accessToken != null && nowInner < exchangeExpiry - EXCHANGE_REFRESH_BUFFER_MS) return

            val apiKeyAuth = config.auth as AuthConfig.ApiKey
            val exchangeBody = json.encodeToString(
                ExchangeRequestWire(key = apiKeyAuth.secretKey, scopes = apiKeyAuth.scopes)
            )

            val response = request<ExchangeResponseWire>(
                method = "POST",
                path = "/api/api-keys/exchange",
                body = exchangeBody,
                skipExchange = true
            )

            require(response.accessToken.isNotBlank()) { "Exchange returned empty access_token" }
            require(response.expiresIn > 0) { "Exchange returned invalid expires_in: ${response.expiresIn}" }

            this.accessToken = response.accessToken
            this.exchangeExpiry = nowInner + (response.expiresIn * 1000L)
        }
    }

    /**
     * Make a GET request
     */
    inline fun <reified T> get(path: String, headers: Map<String, String>? = null): T {
        return request("GET", path, null, headers)
    }

    /**
     * Make a POST request
     */
    inline fun <reified T, reified B> post(path: String, body: B, headers: Map<String, String>? = null): T {
        val jsonBody = json.encodeToString(body)
        return request("POST", path, jsonBody, headers)
    }

    /**
     * Make a PUT request
     */
    inline fun <reified T, reified B> put(
        path: String,
        body: B,
        headers: Map<String, String>? = null,
        retryConfigOverride: RetryConfig? = null,
        retryOnConnectionFailure: Boolean = true
    ): T {
        val jsonBody = json.encodeToString(body)
        return request(
            "PUT",
            path,
            jsonBody,
            headers,
            retryConfigOverride = retryConfigOverride,
            retryOnConnectionFailure = retryOnConnectionFailure
        )
    }

    /**
     * Make a PATCH request
     */
    inline fun <reified T, reified B> patch(path: String, body: B): T {
        val jsonBody = json.encodeToString(body)
        return request("PATCH", path, jsonBody)
    }

    /**
     * Make a DELETE request
     */
    inline fun <reified T> delete(path: String): T {
        return request("DELETE", path, null)
    }

    /**
     * Internal request method
     */
    @PublishedApi
    internal inline fun <reified T> request(
        method: String,
        path: String,
        body: String?,
        headers: Map<String, String>? = null,
        skipExchange: Boolean = false,
        retryConfigOverride: RetryConfig? = null,
        retryOnConnectionFailure: Boolean = true
    ): T {
        if (!skipExchange && config.auth is AuthConfig.ApiKey) {
            ensureExchanged()
        }

        val url = "${baseUrl}$path"

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        accessToken?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }

        headers?.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        // Every idempotent write gets a stable key, whether or not the caller
        // supplied one, so a request is never silently sent without one
        // (servers may reject POST/PUT without Idempotency-Key) and retries of
        // the same call below reuse this same key instead of minting a new one.
        if ((method == "POST" || method == "PUT") && headers?.containsKey("Idempotency-Key") != true) {
            requestBuilder.header("Idempotency-Key", UUID.randomUUID().toString())
        }

        val requestBody = body?.toRequestBody("application/json".toMediaType())

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody())
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody())
            "PATCH" -> requestBuilder.patch(requestBody ?: "".toRequestBody())
            "DELETE" -> requestBuilder.delete(requestBody)
        }

        val request = requestBuilder.build()
        val requestClient = if (retryOnConnectionFailure) {
            client
        } else {
            client.newBuilder().retryOnConnectionFailure(false).build()
        }

        val effectiveRetryConfig = retryConfigOverride ?: retryConfig
        var lastException: Exception? = null
        var currentDelay = effectiveRetryConfig.initialDelayMs

        for (attempt in 0..effectiveRetryConfig.maxRetries) {
            try {
                requestClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        val error = createBloqueError(
                            statusCode = response.code,
                            errorBody = responseBody,
                            defaultMessage = "API error ${response.code}: $responseBody",
                            httpClient = this@BloqueHttpClient
                        )

                        if (response.code in 500..599 && attempt < effectiveRetryConfig.maxRetries) {
                            lastException = error
                            Thread.sleep(currentDelay)
                            currentDelay = minOf(
                                (currentDelay * effectiveRetryConfig.backoffMultiplier).toLong(),
                                effectiveRetryConfig.maxDelayMs
                            )
                            return@use
                        }

                        throw error
                    }

                    if (responseBody.isNullOrEmpty()) {
                        throw BloqueSerializationException("Empty response body")
                    }

                    try {
                        return json.decodeFromString<T>(responseBody)
                    } catch (e: Exception) {
                        throw BloqueSerializationException("Failed to parse response: ${e.message}", e)
                    }
                }
            } catch (e: IOException) {
                lastException = BloqueNetworkError("Network error: ${e.message}", e)
                if (attempt < effectiveRetryConfig.maxRetries) {
                    Thread.sleep(currentDelay)
                    currentDelay = minOf(
                        (currentDelay * effectiveRetryConfig.backoffMultiplier).toLong(),
                        effectiveRetryConfig.maxDelayMs
                    )
                    continue
                }
                throw lastException!!
            } catch (e: BloqueException) {
                throw e
            } catch (e: Exception) {
                throw BloqueException("Unexpected error: ${e.message}", e)
            }
        }

        throw lastException ?: BloqueException("Request failed after ${effectiveRetryConfig.maxRetries} retries")
    }
}
