package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for Virtual account operations
 */
class VirtualClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Create a new Virtual account
     *
     * @param params Parameters for account creation
     * @return The created VirtualAccount
     */
    fun create(params: CreateVirtualAccountParams): VirtualAccount {
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

        val response = httpClient.post<CreateAccountResponse<VirtualDetails>, CreateAccountRequest>(
            path = "/api/mediums/virtual",
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
    fun updateMetadata(params: UpdateVirtualMetadataParams): VirtualAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: Map<String, String?>)

        val response = httpClient.put<CreateAccountResponse<VirtualDetails>, UpdateMetadataRequest>(
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
    fun activate(urn: String): VirtualAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<VirtualDetails>, StatusRequest>(
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
    fun freeze(urn: String): VirtualAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<VirtualDetails>, StatusRequest>(
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
    fun disable(urn: String): VirtualAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.put<CreateAccountResponse<VirtualDetails>, StatusRequest>(
            path = "/api/accounts/$urn/status",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<VirtualDetails>): VirtualAccount {
        return VirtualAccount(
            urn = account.urn,
            id = account.id,
            accountNumber = account.details.accountNumber,
            routingNumber = account.details.routingNumber,
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
