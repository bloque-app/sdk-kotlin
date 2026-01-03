package app.bloque.sdk.accounts

import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for Bancolombia account operations
 */
class BancolombiaClient internal constructor(
    private val httpClient: BloqueHttpClient
) {
    /**
     * Create a new Bancolombia account
     *
     * @param params Optional parameters for account creation
     * @return The created BancolombiaAccount
     */
    fun create(params: CreateBancolombiaAccountParams = CreateBancolombiaAccountParams()): BancolombiaAccount {
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

        val response = httpClient.post<CreateAccountResponse<BancolombiaDetails>, CreateAccountRequest>(
            path = "/api/mediums/bancolombia",
            body = request
        )

        return mapAccountResponse(response.result.account)
    }

    private fun mapAccountResponse(account: AccountData<BancolombiaDetails>): BancolombiaAccount {
        return BancolombiaAccount(
            urn = account.urn,
            id = account.id,
            referenceCode = account.details.referenceCode,
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
