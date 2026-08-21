package app.bloque.sdk.identity

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeysOriginOperatorTest {
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
    fun unboundExchangeOmitsAsIdentity() {
        val http = originKeyClient("user-jwt")
        val apiKeys = ApiKeysClient(http)

        server.enqueue(
            json("""{"access_token":"ex-jwt","expires_in":900,"token_type":"Bearer"}""")
        )

        val result = apiKeys.exchange(ExchangeApiKeyParams(key = "sk_test_unbound"))
        assertEquals("ex-jwt", result.accessToken)

        val recorded = server.takeRequest()
        assertEquals("/api/api-keys/exchange", recorded.path)
        val body = recorded.body.readUtf8()
        assertFalse(body.contains("as_identity"), body)
        assertTrue(body.contains("\"key\":\"sk_test_unbound\""), body)
        assertEquals("Bearer user-jwt", recorded.getHeader("Authorization"))
    }

    @Test
    fun exchangeSendsAsIdentityAndOperatorBearer() {
        val http = originKeyClient("user-jwt")
        http.pinAccessToken("op-jwt")
        val apiKeys = ApiKeysClient(http)

        server.enqueue(
            json("""{"access_token":"impersonation-jwt","expires_in":900,"token_type":"Bearer"}""")
        )

        val result = apiKeys.exchange(
            ExchangeApiKeyParams(
                key = "sk_test_bound",
                scopes = listOf("payments.read"),
                asIdentity = "did:bloque:colocapay:cust"
            )
        )
        assertEquals("impersonation-jwt", result.accessToken)

        val recorded = server.takeRequest()
        assertEquals("/api/api-keys/exchange", recorded.path)
        assertEquals("Bearer op-jwt", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"as_identity\":\"did:bloque:colocapay:cust\""), body)
        assertTrue(body.contains("\"key\":\"sk_test_bound\""), body)
        assertTrue(body.contains("payments.read"), body)
    }

    @Test
    fun createWhilePinnedSendsOperatorBearerUnwrappedResponse() {
        val http = originKeyClient("user-jwt")
        http.pinAccessToken("op-jwt")
        val apiKeys = ApiKeysClient(http)

        server.enqueue(
            json(
                """{"key_id":"sk_test_abc123","secret_key":"sk_test_abc123secret","publishable_key":"pk_test_xyz"}"""
            )
        )

        val created = apiKeys.create(
            CreateApiKeyParams(
                name = "cs-readonly",
                scopes = listOf("identity.read.origin", "alias.find.origin"),
                domains = emptyList(),
                expiration = "never"
            )
        )
        assertEquals("sk_test_abc123", created.keyId)
        assertEquals("sk_test_abc123secret", created.secretKey)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/api-keys", recorded.path)
        assertEquals("Bearer op-jwt", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"cs-readonly\""), body)
        assertTrue(body.contains("identity.read.origin"), body)
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
