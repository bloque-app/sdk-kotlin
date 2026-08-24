package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Client for `external-us-bank` (Brale/Plaid-linked bank account) operations.
 *
 * Linking flow:
 * 1. [create] starts the linkage and returns a pending account with a
 *    `linkToken`/`linkUrl`/`jwt`.
 * 2. Open [plaidLinkUrl] (or [ExternalUsBankAccount.linkUrl] directly) in a
 *    browser/webview so the holder completes Plaid Link.
 * 3. The hosted page calls [completePlaidLink] itself, or you call it
 *    manually with the resulting `public_token`.
 * 4. Once `linkStatus` is `"active"`, use [pull] to proactively debit the
 *    linked bank via ACH and auto-swap to DUSD on Kusama.
 */
class ExternalUsBankClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    private fun mapToJsonElement(map: Map<String, Any?>?): JsonElement {
        if (map == null) return JsonObject(emptyMap())
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is Map<*, *> -> put(key, mapToJsonElement(value as Map<String, Any?>))
                    is List<*> -> put(key, buildJsonArray {
                        value.forEach { item ->
                            when (item) {
                                is String -> add(JsonPrimitive(item))
                                is Number -> add(JsonPrimitive(item))
                                is Boolean -> add(JsonPrimitive(item))
                                is Map<*, *> -> add(mapToJsonElement(item as Map<String, Any?>))
                                else -> add(JsonPrimitive(item.toString()))
                            }
                        }
                    })
                    else -> put(key, value?.toString() ?: "")
                }
            }
        }
    }

    /**
     * Start linking an external US bank account.
     *
     * Only starts the linkage — the returned account is `pending_link` with
     * a `linkToken`/`linkUrl`/`jwt` for the holder to complete Plaid Link via
     * [plaidLinkUrl] and [completePlaidLink].
     *
     * @param params Label, return URL, and optional account-creation parameters
     * @return The pending ExternalUsBankAccount
     */
    fun create(params: CreateExternalUsBankAccountParams = CreateExternalUsBankAccountParams()): ExternalUsBankAccount {
        val request = CreateAccountRequest(
            holderUrn = params.holderUrn ?: httpClient.getUrn() ?: "",
            webhookUrl = params.webhookUrl,
            ledgerAccountId = params.ledgerId,
            input = buildJsonObject {
                params.label?.let { put("label", it) }
                params.returnUrl?.let { put("return_url", it) }
                params.state?.let { put("state", it) }
            },
            metadata = buildJsonObject {
                put("source", "sdk-kotlin")
                val metaJson = mapToJsonElement(params.metadata)
                if (metaJson is JsonObject) {
                    metaJson.entries.forEach { put(it.key, it.value) }
                }
            }
        )

        val headers = params.idempotencyKey?.let { mapOf("Idempotency-Key" to it) }

        val response = httpClient.post<CreateAccountResponse<ExternalUsBankDetails>, CreateAccountRequest>(
            path = "/api/mediums/external-us-bank",
            body = request,
            headers = headers
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * List external-us-bank accounts.
     *
     * @param params Optional filter parameters
     * @return List of external-us-bank accounts
     */
    @JvmOverloads
    fun list(params: ListExternalUsBankParams = ListExternalUsBankParams()): List<ExternalUsBankAccount> {
        @Serializable
        data class ExternalUsBankListResponse(val accounts: List<AccountData<ExternalUsBankDetails>>)

        val holderUrn = params.holderUrn ?: httpClient.getUrn()

        val queryParams = buildString {
            append("?medium=external-us-bank")
            holderUrn?.let { append("&holder_urn=$it") }
            params.urn?.let { append("&urn=$it") }
            params.status?.let { append("&status=$it") }
        }

        val response = httpClient.get<ExternalUsBankListResponse>(
            path = "/api/accounts$queryParams"
        )

        return response.accounts.map { account -> mapAccountResponse(account) }
    }

    /**
     * Get an external-us-bank account by URN.
     *
     * @param urn Account URN
     * @return The external-us-bank account
     */
    fun get(urn: String): ExternalUsBankAccount {
        @Serializable
        data class GetAccountResponse(val account: AccountData<ExternalUsBankDetails>)

        val response = httpClient.get<GetAccountResponse>(
            path = "/api/accounts/$urn"
        )
        return mapAccountResponse(response.account)
    }

    /**
     * Update account metadata
     *
     * @param params Parameters with URN and new metadata
     * @return Updated account
     */
    fun updateMetadata(params: UpdateExternalUsBankMetadataParams): ExternalUsBankAccount {
        @Serializable
        data class UpdateMetadataRequest(val metadata: JsonElement)

        val response = httpClient.patch<CreateAccountResponse<ExternalUsBankDetails>, UpdateMetadataRequest>(
            path = "/api/accounts/${params.urn}",
            body = UpdateMetadataRequest(mapToJsonElement(params.metadata))
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Activate account
     */
    fun activate(urn: String): ExternalUsBankAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<ExternalUsBankDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("active")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Freeze account
     */
    fun freeze(urn: String): ExternalUsBankAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<ExternalUsBankDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("frozen")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Disable account
     */
    fun disable(urn: String): ExternalUsBankAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<ExternalUsBankDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("disabled")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Delete account
     */
    fun delete(urn: String): ExternalUsBankAccount {
        @Serializable
        data class StatusRequest(@SerialName("status") val status: String)

        val response = httpClient.patch<CreateAccountResponse<ExternalUsBankDetails>, StatusRequest>(
            path = "/api/accounts/$urn",
            body = StatusRequest("deleted")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Builds the path (and query string) for the Plaid Link hosted page —
     * a browser/webview-only HTML page, not a JSON API, so this does not
     * make an HTTP call.
     *
     * Returns a path relative to your configured API host (e.g.
     * `Mode.SANDBOX.baseUrl` / `Mode.PRODUCTION.baseUrl` from `BloqueConfig`)
     * rather than an absolute URL, since the SDK session does not expose its
     * resolved base URL publicly — prepend it yourself:
     * `mode.baseUrl + session.accounts.externalUsBank.plaidLinkUrl(params)`.
     *
     * @param params Account URN, link token, and JWT from [create]'s response
     * @return Path (with query string) to open in a browser/webview to complete Plaid Link
     */
    fun plaidLinkUrl(params: PlaidLinkUrlParams): String {
        fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

        val query = buildString {
            append("token=").append(enc(params.jwt))
            append("&link_token=").append(enc(params.linkToken))
            append("&account_urn=").append(enc(params.accountUrn))
            params.returnUrl?.let { append("&return_url=").append(enc(it)) }
            params.state?.let { append("&state=").append(enc(it)) }
        }

        return "/api/mediums/external-us-bank/plaid-link?$query"
    }

    /**
     * Complete Plaid Link onboarding by exchanging Plaid's `public_token`.
     *
     * This call is authenticated with the short-lived plaid-link JWT from
     * [ExternalUsBankAccount.jwt] instead of the SDK session's own bearer
     * token — it is bound to a single [CompletePlaidLinkParams.accountUrn].
     *
     * @param params Account URN, Plaid `public_token`, and the plaid-link JWT
     * @return The now-active ExternalUsBankAccount
     */
    fun completePlaidLink(params: CompletePlaidLinkParams): ExternalUsBankAccount {
        val response = httpClient.post<CreateAccountResponse<ExternalUsBankDetails>, CompletePlaidLinkRequest>(
            path = "/api/mediums/external-us-bank/complete",
            body = CompletePlaidLinkRequest(
                accountUrn = params.accountUrn,
                publicToken = params.publicToken
            ),
            headers = mapOf("Authorization" to "Bearer ${params.jwt}")
        )

        return mapAccountResponse(response.result.account)
    }

    /**
     * Proactively pull funds from a linked US bank via Brale `ach_debit` and
     * swap directly to DUSD on Kusama Asset Hub, teleporting the proceeds to
     * the caller's Kreivo ledger account. Requires an `"active"` linkage.
     *
     * @param params Account URN, USD decimal amount, and optional idempotency key
     * @return Order signature, graph id, status, and (if present) the
     * auto-executed first node's execution result
     */
    fun pull(params: PullExternalUsBankParams): PullExternalUsBankResult {
        val response = httpClient.post<PullExternalUsBankResponseWire, PullExternalUsBankRequest>(
            path = "/api/mediums/external-us-bank/${params.accountUrn}/pull",
            body = PullExternalUsBankRequest(
                amount = params.amount,
                idempotencyKey = params.idempotencyKey
            )
        )

        val result = response.result
        return PullExternalUsBankResult(
            orderSig = result?.orderSig,
            graphId = result?.graphId,
            status = result?.status,
            execution = result?.execution
        )
    }

    private fun mapAccountResponse(account: AccountData<ExternalUsBankDetails>): ExternalUsBankAccount {
        return ExternalUsBankAccount(
            urn = account.urn,
            id = account.id,
            linkStatus = account.details.linkStatus,
            braleAccountId = account.details.braleAccountId,
            braleAddressId = account.details.braleAddressId,
            bankAccountLast4 = account.details.bankAccountLast4,
            bankName = account.details.bankName,
            linkToken = account.details.linkToken,
            linkTokenExpiration = account.details.linkTokenExpiration,
            linkUrl = account.details.linkUrl,
            jwt = account.details.jwt,
            failureReason = account.details.failureReason,
            owner = account.details.owner,
            routingNumber = account.details.routingNumber,
            accountNumber = account.details.accountNumber,
            accountType = account.details.accountType,
            bankAddress = account.details.bankAddress,
            beneficiaryAddress = account.details.beneficiaryAddress,
            transferTypes = account.details.transferTypes,
            needsUpdate = account.details.needsUpdate,
            lastUpdated = account.details.lastUpdated,
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
