package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

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
     * Request an assertion (authentication challenge) for an alias
     *
     * @param alias The user alias to authenticate
     * @return Assertion result with challenge details
     */
    fun assert(alias: String): AssertionResult {
        val response = httpClient.post<AssertionResponseWire, Map<String, String>>(
            path = "/api/origins/$originName/assert",
            body = mapOf("alias" to alias)
        )

        return AssertionResult(
            challengeType = ChallengeType.valueOf(response.result.challengeType),
            value = response.result.value
        )
    }
}
