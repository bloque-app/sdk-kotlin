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
     * @param options Optional settings to wait for account activation
     * @return The created VirtualAccount
     *
     * Example usage:
     * ```kotlin
     * // Create without waiting
     * val account = session.accounts.virtual.create(
     *     CreateVirtualAccountParams(name = "My Account")
     * )
     *
     * // Create and wait for active status with ledger
     * val account = session.accounts.virtual.create(
     *     CreateVirtualAccountParams(name = "My Account"),
     *     CreateAccountOptions(waitLedger = true)
     * )
     * ```
     */
    @JvmOverloads
    fun create(
        params: CreateVirtualAccountParams,
        options: CreateAccountOptions? = null
    ): VirtualAccount {
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

        val response = httpClient.post<CreateAccountResponse<VirtualDetails>, CreateAccountRequest>(
            path = "/api/mediums/virtual",
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
     * List virtual accounts
     *
     * @param params Optional filter parameters
     * @return List of virtual accounts
     */
    @JvmOverloads
    fun list(params: ListVirtualParams = ListVirtualParams()): List<VirtualAccount> {
        @Serializable
        data class VirtualListResponse(val accounts: List<AccountData<VirtualDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=virtual")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<VirtualListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get a virtual account by URN
     *
     * @param urn Account URN
     * @return The virtual account
     */
    fun get(urn: String): VirtualAccount {
        val accounts = list(ListVirtualParams(urn = urn))
        return accounts.firstOrNull()
            ?: throw RuntimeException("Account not found. URN: $urn")
    }

    /**
     * Private method to poll account status until it becomes active
     */
    private fun waitForActiveStatus(urn: String, timeout: Long): VirtualAccount {
        val startTime = System.currentTimeMillis()
        val pollingInterval = 2000L // 2 seconds

        while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                throw RuntimeException("Timeout waiting for account to become active. URN: $urn")
            }

            val result = list(ListVirtualParams(urn = urn))
            val account = result.firstOrNull()
                ?: throw RuntimeException("Account not found. URN: $urn")

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
            firstName = account.details.firstName,
            lastName = account.details.lastName,
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
