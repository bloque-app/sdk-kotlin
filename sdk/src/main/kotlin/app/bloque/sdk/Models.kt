package app.bloque.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConnectRequest(
    @SerialName("assertion_result") val assertionResult: AssertionResult,
    @SerialName("extra_context") val extraContext: Map<String, String> = emptyMap()
)

@Serializable
internal data class AssertionResult(
    val challengeType: String,
    val value: AssertionValue
)

@Serializable
internal data class AssertionValue(
    @SerialName("api_key") val apiKey: String,
    val alias: String
)

@Serializable
internal data class ConnectResponse(
    val result: ConnectResult
)

@Serializable
internal data class ConnectResult(
    @SerialName("access_token") val accessToken: String
)
