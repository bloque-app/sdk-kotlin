package app.bloque.sdk.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * HTTP client for making API requests
 */
class BloqueHttpClient(
    private val config: BloqueConfig
) {
    @PublishedApi
    internal var accessToken: String? = null
        private set

    private var urn: String? = null

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

    val origin: String get() = config.origin

    fun getUrn(): String? = urn

    fun updateAccessToken(token: String) {
        this.accessToken = token
    }

    fun updateUrn(urn: String) {
        this.urn = urn
    }

    /**
     * Make a GET request
     */
    inline fun <reified T> get(path: String): T {
        return request("GET", path, null)
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
    inline fun <reified T, reified B> put(path: String, body: B, headers: Map<String, String>? = null): T {
        val jsonBody = json.encodeToString(body)
        return request("PUT", path, jsonBody, headers)
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
    internal inline fun <reified T> request(method: String, path: String, body: String?, headers: Map<String, String>? = null): T {
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

        val requestBody = body?.toRequestBody("application/json".toMediaType())

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody())
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody())
            "PATCH" -> requestBuilder.patch(requestBody ?: "".toRequestBody())
            "DELETE" -> requestBuilder.delete(requestBody)
        }

        val request = requestBuilder.build()

        var lastException: Exception? = null
        var currentDelay = retryConfig.initialDelayMs

        for (attempt in 0..retryConfig.maxRetries) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        val error = createBloqueError(
                            statusCode = response.code,
                            errorBody = responseBody,
                            defaultMessage = "API error ${response.code}: $responseBody"
                        )

                        // Only retry on server errors (5xx), not client errors (4xx)
                        if (response.code in 500..599 && attempt < retryConfig.maxRetries) {
                            lastException = error
                            Thread.sleep(currentDelay)
                            currentDelay = minOf(
                                (currentDelay * retryConfig.backoffMultiplier).toLong(),
                                retryConfig.maxDelayMs
                            )
                            return@use // Continue to next attempt
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
                // Retry on network errors
                lastException = BloqueNetworkError("Network error: ${e.message}", e)
                if (attempt < retryConfig.maxRetries) {
                    Thread.sleep(currentDelay)
                    currentDelay = minOf(
                        (currentDelay * retryConfig.backoffMultiplier).toLong(),
                        retryConfig.maxDelayMs
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

        // If we get here, all retries failed
        throw lastException ?: BloqueException("Request failed after ${retryConfig.maxRetries} retries")
    }
}
