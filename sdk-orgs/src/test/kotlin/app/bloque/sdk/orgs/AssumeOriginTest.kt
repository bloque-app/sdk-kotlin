package app.bloque.sdk.orgs

import app.bloque.sdk.core.BloqueConfig
import app.bloque.sdk.core.BloqueHttpClient
import app.bloque.sdk.core.Mode
import app.bloque.sdk.core.RetryConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AssumeOriginTest {
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
    fun assumeOriginPostsNamespaceAsAndPinsOperatorJwt() {
        val http = originKeyClient(token = "user-jwt")
        val orgs = OrgsClient(http)

        server.enqueue(
            json("""{"access_token":"op-jwt","expires_in":900,"token_type":"Bearer"}""")
        )
        server.enqueue(
            json("""{"access_token":"op-jwt-2","expires_in":900,"token_type":"Bearer"}""")
        )

        val assumed = orgs.assumeOrigin("colocapay")
        assertEquals("op-jwt", assumed.accessToken)
        assertEquals(900, assumed.expiresIn)
        assertEquals("Bearer", assumed.tokenType)

        orgs.assumeOrigin("colocapay")

        val first = server.takeRequest()
        assertEquals("POST", first.method)
        assertEquals("/api/origins/colocapay/as", first.path)
        assertEquals("Bearer user-jwt", first.getHeader("Authorization"))

        val second = server.takeRequest()
        assertEquals("/api/origins/colocapay/as", second.path)
        assertEquals("Bearer op-jwt", second.getHeader("Authorization"))
    }

    private fun originKeyClient(token: String): BloqueHttpClient {
        val config = BloqueConfig.builder()
            .origin("test-origin")
            .originKey("test-origin-key")
            .mode(Mode.SANDBOX)
            .baseUrl(server.url("/").toString())
            .retry(RetryConfig.NONE)
            .timeoutMs(5_000)
            .build()
        val http = BloqueHttpClient(config)
        http.updateAccessToken(token)
        return http
    }

    private fun json(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
