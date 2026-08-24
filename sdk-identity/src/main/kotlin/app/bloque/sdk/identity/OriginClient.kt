package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import java.net.URLEncoder

/**
 * Generic client for origin-specific operations
 *
 * @param TAssertion The type of assertion this origin uses
 */
class OriginClient<TAssertion : OTPAssertion> constructor(
    httpClient: BloqueHttpClient,
    private val originName: String
) : BaseClient(httpClient) {

    /**
     * Request an attestation challenge for registering a new identity.
     *
     * @param alias Optional identity alias/identifier for attestation (e.g. a wallet address)
     * @return The challenge to resolve and send back as `assertion_result` on [OriginsClient.register]
     */
    @JvmOverloads
    fun attest(alias: String? = null): Challenge {
        val query = alias?.let { "?alias=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
        return httpClient.get<Challenge>(path = "/api/origins/$originName/attest$query")
    }

    /**
     * Request an assertion (authentication challenge) for an alias
     *
     * @param alias The user alias to authenticate
     * @return The challenge to resolve and send back as `assertion_result` on [connect]
     */
    fun assert(alias: String): Challenge {
        val query = "?alias=${URLEncoder.encode(alias, "UTF-8")}"
        return httpClient.get<Challenge>(path = "/api/origins/$originName/assert$query")
    }

    /**
     * Resolve an assertion challenge (from [assert]) and connect to an
     * existing identity. Supports any assertion result type — an OTP code,
     * a signature, an API key, etc. — unlike
     * [app.bloque.sdk.BloqueSDK.connect], which only handles the legacy
     * API_KEY flow directly.
     *
     * @param params The resolved challenge and any extra context
     * @return Access token for the connected identity
     */
    fun connect(params: ConnectParams): ConnectResult {
        val request = ConnectRequestWire(
            assertionResult = GenericAssertionResultWire(
                alias = params.alias,
                challengeType = params.challengeType.name,
                value = params.value,
                originalChallengeParams = params.originalChallengeParams
            ),
            extraContext = params.metadata ?: emptyMap()
        )

        val response = httpClient.post<ConnectResponseWire, ConnectRequestWire>(
            path = "/api/origins/$originName/connect",
            body = request
        )

        return ConnectResult(
            accessToken = response.result.accessToken,
            reqId = response.reqId
        )
    }
}
