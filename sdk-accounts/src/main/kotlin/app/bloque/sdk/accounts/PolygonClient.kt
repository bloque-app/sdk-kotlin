package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for Polygon wallet account operations
 */
class PolygonClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a new Polygon wallet account
     *
     * @param params Optional parameters for account creation
     * @return The created PolygonAccount
     */
    @JvmOverloads
    fun create(params: CreatePolygonAccountParams = CreatePolygonAccountParams()): PolygonAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = emptyMap(),
            metadata = buildMap {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                putAll(params.metadata)
            }
        )

        val response = httpClient.post<CreateAccountResponse<PolygonDetails>, CreateAccountRequest>(
            path = "/api/mediums/polygon",
            body = request
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Update account metadata
     *
     * @param params Parameters with URN and new metadata
     * @return Updated account
     */
    fun updateMetadata(params: UpdatePolygonMetadataParams): PolygonAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: Map<String, String?>)

        val response = httpClient.put<CreateAccountResponse<PolygonDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}/metadata",
            body = UpdateMetadataRequest(params.metadata)
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Activate account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun activate(urn: String): PolygonAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("active")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Freeze account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun freeze(urn: String): PolygonAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("frozen")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Disable account
     *
     * @param urn Account URN
     * @return Updated account
     */
    fun disable(urn: String): PolygonAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<PolygonDetails>): PolygonAccount {
        return PolygonAccount(
            urn = account.urn,
            id = account.id,
            walletAddress = account.details.walletAddress,
            status = account.status,
            ownerUrn = account.ownerUrn,
            ledgerId = account.ledgerAccountId,
            webhookUrl = account.webhookUrl,
            metadata = account.metadata,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
