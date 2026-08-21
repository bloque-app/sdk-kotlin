package app.bloque.sdk.core

import kotlinx.serialization.Serializable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PinAccessTokenTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun pinAccessTokenSkipsApiKeyAutoExchange() {
        val http = BloqueHttpClient(
            BloqueConfig.builder()
                .secretKey("sk_test_placeholder_not_a_real_key")
                .mode(Mode.SANDBOX)
                .baseUrl(server.url("/").toString())
                .retry(RetryConfig.NONE)
                .timeoutMs(5_000)
                .build()
        )

        server.enqueue(json("""{"ok":true}"""))
        http.pinAccessToken("op-jwt")
        http.get<PingWire>("/api/ping")

        assertEquals(1, server.requestCount)
        val recorded = server.takeRequest()
        assertEquals("/api/ping", recorded.path)
        assertEquals("Bearer op-jwt", recorded.getHeader("Authorization"))
    }

    @Test
    fun updateAccessTokenClearsPinAndResumesAutoExchange() {
        val http = BloqueHttpClient(
            BloqueConfig.builder()
                .secretKey("sk_test_placeholder_not_a_real_key")
                .mode(Mode.SANDBOX)
                .baseUrl(server.url("/").toString())
                .retry(RetryConfig.NONE)
                .timeoutMs(5_000)
                .build()
        )

        http.pinAccessToken("op-jwt")
        http.updateAccessToken("stale-user-jwt")

        server.enqueue(
            json("""{"access_token":"fresh-sk-jwt","expires_in":900,"token_type":"Bearer"}""")
        )
        server.enqueue(json("""{"ok":true}"""))
        http.get<PingWire>("/api/ping")

        assertEquals(2, server.requestCount)
        val exchange = server.takeRequest()
        assertEquals("/api/api-keys/exchange", exchange.path)
        val ping = server.takeRequest()
        assertEquals("/api/ping", ping.path)
        assertEquals("Bearer fresh-sk-jwt", ping.getHeader("Authorization"))
    }

    @Serializable
    private data class PingWire(val ok: Boolean)

    private fun json(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
