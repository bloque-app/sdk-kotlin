package app.bloque.sdk.accounts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ============================================
// Request Models
// ============================================

@Serializable
data class CreateAccountRequest(
    @SerialName("holder_urn") val holderUrn: String,
    @SerialName("webhook_url") val webhookUrl: String? = null,
    @SerialName("ledger_account_id") val ledgerAccountId: String? = null,
    val input: JsonElement? = null,
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
    val metadata: Map<String, String?>? = null
)

data class ListBancolombiaParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

// ============================================
// Card Account Models
// ============================================

@Serializable
data class CardDetails(
    @SerialName("card_last_four") val lastFour: String? = null,
    @SerialName("card_product_type") val productType: String? = null,
    @SerialName("card_type") val cardType: String? = null,
    @SerialName("card_url_details") val detailsUrl: String? = null
)

data class CardAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val lastFour: String?,
    val productType: String?,
    val cardType: String?,
    val detailsUrl: String?,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: Map<String, String?> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CreateCardAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, String?>? = null
)

data class ListCardParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateCardMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, String?>
)

// ============================================
// Virtual Account Models
// ============================================

@Serializable
data class VirtualDetails(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null
)

data class VirtualAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: Map<String, String?> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CreateVirtualAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, String?>? = null
)

data class ListVirtualParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateVirtualMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, String?>
)

/**
 * Options for account creation with optional wait for ledger
 */
data class CreateAccountOptions @JvmOverloads constructor(
    /** Whether to wait for the account to become active and have a ledger assigned */
    val waitLedger: Boolean = false,
    /** Timeout in milliseconds for waiting (default: 60000ms = 60 seconds) */
    val timeout: Long = 60000L
)

// ============================================
// Polygon Account Models
// ============================================

@Serializable
data class PolygonDetails(
    val address: String? = null,
    val network: String? = null
)

data class PolygonAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val address: String?,
    val network: String?,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: Map<String, String?> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CreatePolygonAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, String?>? = null
)

data class UpdatePolygonMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, String?>
)

data class ListPolygonParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateBancolombiaMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, String?>
)

// ============================================
// Transfer Models
// ============================================

/**
 * Supported assets for transfers
 */
enum class SupportedAsset(val value: String) {
    DUSD_6("DUSD/6"),
    KSM_12("KSM/12");

    override fun toString(): String = value

    companion object {
        @JvmStatic
        fun fromString(value: String): SupportedAsset {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Unknown asset: $value")
        }
    }
}

data class TransferParams @JvmOverloads constructor(
    val sourceUrn: String,
    val destinationUrn: String,
    val amount: String,
    val asset: SupportedAsset,
    val metadata: Map<String, Any?> = emptyMap()
)

@Serializable
internal data class TransferRequest(
    @SerialName("source_urn") val sourceUrn: String,
    @SerialName("destination_urn") val destinationUrn: String,
    val amount: String,
    val asset: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
internal data class TransferResponseData(
    @SerialName("queue_id") val queueId: String,
    val status: String,
    val message: String
)

@Serializable
internal data class TransferResponseWrapper(
    val result: TransferResponseData
)

data class TransferResult constructor(
    val queueId: String,
    val status: String,
    val message: String
)

// ============================================
// Balance and Movement Models
// ============================================

data class TokenBalance constructor(
    val current: String,
    val pending: String,
    val `in`: String,
    val out: String
)

@Serializable
internal data class TokenBalanceWire(
    val current: String,
    val pending: String,
    @SerialName("in") val inAmount: String,
    @SerialName("out") val outAmount: String
)

data class GetBalanceParams @JvmOverloads constructor(
    val urn: String,
    val asset: SupportedAsset? = null
)

data class ListMovementsParams @JvmOverloads constructor(
    val urn: String,
    val limit: Int? = null,
    val offset: Int? = null
)

@Serializable
data class CardMovement(
    val id: String,
    val amount: String,
    val asset: String,
    @SerialName("from_account_id") val fromAccountId: String,
    @SerialName("to_account_id") val toAccountId: String,
    val direction: String,
    val reference: String,
    @SerialName("rail_name") val railName: String,
    @SerialName("created_at") val createdAt: String
)
