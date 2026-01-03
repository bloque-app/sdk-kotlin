package app.bloque.sdk.accounts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================
// Request Models
// ============================================

@Serializable
data class CreateAccountRequest(
    @SerialName("holder_urn") val holderUrn: String,
    @SerialName("webhook_url") val webhookUrl: String? = null,
    @SerialName("ledger_account_id") val ledgerAccountId: String? = null,
    val input: Map<String, String> = emptyMap(),
    val metadata: Map<String, String?> = emptyMap()
)

// ============================================
// Response Models
// ============================================

@Serializable
data class CreateAccountResponse<T>(
    val result: AccountResult<T>
)

@Serializable
data class AccountResult<T>(
    val account: AccountData<T>
)

@Serializable
data class AccountData<T>(
    val urn: String,
    val id: String,
    val status: String,
    @SerialName("owner_urn") val ownerUrn: String,
    @SerialName("ledger_account_id") val ledgerAccountId: String? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null,
    val metadata: Map<String, String?> = emptyMap(),
    val details: T,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

// ============================================
// Bancolombia Specific
// ============================================

@Serializable
data class BancolombiaDetails(
    @SerialName("reference_code") val referenceCode: String? = null
)

/**
 * Bancolombia account representation for SDK users
 */
data class BancolombiaAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val referenceCode: String?,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: Map<String, String?> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * Parameters for creating a Bancolombia account
 */
data class CreateBancolombiaAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, String?> = emptyMap()
)
