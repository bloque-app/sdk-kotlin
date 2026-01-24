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
     * @param options Optional settings to wait for account activation
     * @return The created PolygonAccount
     *
     * Example usage:
     * ```kotlin
     * // Create without waiting
     * val account = session.accounts.polygon.create(
     *     CreatePolygonAccountParams(name = "My Wallet")
     * )
     *
     * // Create and wait for active status with ledger
     * val account = session.accounts.polygon.create(
     *     CreatePolygonAccountParams(name = "My Wallet"),
     *     CreateAccountOptions(waitLedger = true)
     * )
     * ```
     */
    @JvmOverloads
    fun create(
        params: CreatePolygonAccountParams = CreatePolygonAccountParams(),
        options: CreateAccountOptions? = null
    ): PolygonAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = null,
            metadata = buildMap {
                put("source", "sdk-kotlin")
                params.name?.let { put("name", it) }
                params.metadata?.let { putAll(it) }
            }
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateAccountResponse<PolygonDetails>, CreateAccountRequest>(
            path = "/api/mediums/polygon",
            body = request,
            headers = headers
        )

        val account = mapAccountResponse(response.result.account)

        if (options?.waitLedger == true) {
            return waitForActiveStatus(account.urn, options.timeout)
        }

        return account
    }

    /**
     * List polygon accounts
     *
     * @param params Optional filter parameters
     * @return List of polygon accounts
     */
    @JvmOverloads
    fun list(params: ListPolygonParams = ListPolygonParams()): List<PolygonAccount> {
        @Serializable
        data class PolygonListResponse(val accounts: List<AccountData<PolygonDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=polygon")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<PolygonListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a polygon account by URN
     *
     * @param urn Account URN
     * @return The polygon account
     */
    fun get(urn: String): PolygonAccount {
        val response = httpClient.get<CreateAccountResponse<PolygonDetails>>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.result.account)
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): PolygonAccount {
        val startTime = System.currentTimeMillis()
        val pollingInterval = 2000L // 2 seconds

        while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                throw RuntimeException("Timeout waiting for account to become active. URN: $urn")
            }

            val account = get(urn)

            if (account.status == "active") {
                return account
            }

            if (account.status == "creation_failed") {
                throw RuntimeException("Account creation failed. URN: $urn")
            }

            Thread.sleep(pollingInterval)
        }
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

        val response = httpClient.patch<CreateAccountResponse<PolygonDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
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

        val response = httpClient.patch<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
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

        val response = httpClient.patch<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
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

        val response = httpClient.patch<CreateAccountResponse<PolygonDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<PolygonDetails>): PolygonAccount {
        return PolygonAccount(
            urn = account.urn,
            id = account.id,
            address = account.details.address,
            network = account.details.network,
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
