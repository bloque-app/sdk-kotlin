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
    val metadata: JsonElement = JsonObject(emptyMap())
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
    val metadata: JsonElement = JsonObject(emptyMap()),
    val details: T,
    // The mediums API's Account type (accounts.ts) never actually includes
    // created_at/updated_at on any create/get/list response — these are kept
    // nullable (rather than removed) in case a future API revision adds them,
    // and so existing callers referencing `.createdAt`/`.updatedAt` don't
    // need a source change, only a null-check.
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ============================================
// Bancolombia Specific
// ============================================

@Serializable
data class BancolombiaDetails(
    /** Account identifier (same as reference_code) */
    val id: String? = null,
    /** Unique 5-digit reference code for the virtual account */
    @SerialName("reference_code") val referenceCode: String? = null,
    /** Payment agreement code for Bancolombia transactions */
    @SerialName("payment_agreement_code") val paymentAgreementCode: String? = null,
    /** Bank account number */
    @SerialName("bank_account_number") val bankAccountNumber: String? = null,
    /** Bank account type (savings or checking) */
    @SerialName("bank_account_type") val bankAccountType: String? = null,
    /** Bank account holder name */
    @SerialName("bank_account_holder_name") val bankAccountHolderName: String? = null,
    /** Bank account holder ID type (e.g. NIT, CC) */
    @SerialName("bank_account_holder_id_type") val bankAccountHolderIdType: String? = null,
    /** Bank account holder ID value */
    @SerialName("bank_account_holder_id_value") val bankAccountHolderIdValue: String? = null,
    /** Available deposit networks (e.g. bancolombia_a_la_mano, bancolombia_atm) */
    val network: List<String>? = null
)

/**
 * Bancolombia account representation for SDK users
 */
data class BancolombiaAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    /** Unique 5-digit reference code for the virtual account */
    val referenceCode: String?,
    /** Account identifier from details (same as reference_code) */
    val detailsId: String? = null,
    /** Payment agreement code for Bancolombia transactions */
    val paymentAgreementCode: String? = null,
    /** Bank account number */
    val bankAccountNumber: String? = null,
    /** Bank account type (savings or checking) */
    val bankAccountType: String? = null,
    /** Bank account holder name */
    val bankAccountHolderName: String? = null,
    /** Bank account holder ID type (e.g. NIT, CC) */
    val bankAccountHolderIdType: String? = null,
    /** Bank account holder ID value */
    val bankAccountHolderIdValue: String? = null,
    /** Available deposit networks (e.g. bancolombia_a_la_mano, bancolombia_atm) */
    val network: List<String>? = null,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Parameters for creating a Bancolombia account
 */
data class CreateBancolombiaAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
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
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateCardAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class ListCardParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null
)

data class UpdateCardMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

/**
 * Parameters for tokenizing a card for Apple Pay (`POST /accounts/:urn/tokenize/apple`)
 */
data class TokenizeAppleCardParams constructor(
    val urn: String,
    /** Apple Pay certificates for tokenization */
    val certificates: List<String>,
    /** Cryptographic nonce for Apple Pay */
    val nonce: String,
    /** Signature of the nonce for Apple Pay */
    val nonceSignature: String
)

@Serializable
data class AppleCardTokenization(
    @SerialName("activation_data") val activationData: String? = null,
    @SerialName("encrypted_pass_data") val encryptedPassData: String? = null,
    @SerialName("ephemeral_public_key") val ephemeralPublicKey: String? = null
)

/**
 * Parameters for tokenizing a card for Google Pay (`POST /accounts/:urn/tokenize/google`)
 */
data class TokenizeGoogleCardParams constructor(
    val urn: String,
    /** Device ID for Google Pay tokenization */
    val deviceId: String,
    /** Wallet account ID for Google Pay */
    val walletAccountId: String
)

@Serializable
data class GoogleCardTokenization(
    /** One-time passcode for Google Pay */
    val opc: String? = null
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
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateVirtualAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class ListVirtualParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateVirtualMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
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

/**
 * A still-open deposit swap order tracked on a Polygon (EVM) account while a
 * deposit is being swept and swapped. Mirrors `EvmDepositSwapOrder`.
 */
@Serializable
data class PolygonOpenDeposit(
    @SerialName("from_account_id") val fromAccountId: String? = null,
    @SerialName("swept_hash") val sweptHash: String? = null,
    @SerialName("to_ledger_account_id") val toLedgerAccountId: String? = null,
    @SerialName("from_amount") val fromAmount: String? = null
)

@Serializable
data class PolygonDetails(
    /** Account id in the provider (same value as the account's top-level `id`). */
    val id: String? = null,
    val address: String? = null,
    val network: String? = null,
    /** Initial funding transaction hash, when the wallet was seeded on creation. */
    @SerialName("funding_tx") val fundingTx: String? = null,
    @SerialName("owner_urn") val ownerUrn: String? = null,
    /** Deposits swept and swapped but not yet confirmed, keyed by order signature. */
    @SerialName("open_deposits") val openDeposits: Map<String, PolygonOpenDeposit>? = null
)

data class PolygonAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val address: String?,
    val network: String?,
    /** Initial funding transaction hash, when the wallet was seeded on creation. */
    val fundingTx: String? = null,
    /** Deposits swept and swapped but not yet confirmed, keyed by order signature. */
    val openDeposits: Map<String, PolygonOpenDeposit>? = null,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreatePolygonAccountParams @JvmOverloads constructor(
    val name: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class UpdatePolygonMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

data class ListPolygonParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateBancolombiaMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

// ============================================
// BRE-B Account Models
// ============================================

@Serializable
enum class BrebKeyType {
    ID,
    PHONE,
    /** Distinct from [PHONE] — added alongside it in the BRE-B key registry. */
    MOBILE,
    EMAIL,
    ALPHA,
    BCODE
}

data class BrebOperationError constructor(
    val code: String?,
    val message: String
)

data class BrebOperationResult<T> constructor(
    val data: T?,
    val error: BrebOperationError?
)

data class CreateBrebKeyParams @JvmOverloads constructor(
    val keyType: BrebKeyType? = null,
    val key: String,
    val displayName: String? = null,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: Map<String, Any?>? = null
)

data class ResolveBrebKeyParams constructor(
    val keyType: BrebKeyType? = null,
    val key: String
)

data class DeleteBrebKeyParams constructor(
    val accountUrn: String
)

data class SuspendBrebKeyParams constructor(
    val accountUrn: String
)

data class ActivateBrebKeyParams constructor(
    val accountUrn: String
)

data class ListBrebKeyParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

/**
 * Parameters for decoding a BRE-B EMVCo QR string (`POST /mediums/breb/decode-qr`)
 */
data class DecodeBrebQrParams constructor(
    /** EMVCo string extracted from the BRE-B QR code */
    val qrCodeData: String
)

@Serializable
internal data class DecodeBrebQrRequest(
    @SerialName("qr_code_data") val qrCodeData: String
)

@Serializable
data class BrebQrKeyInfo(
    @SerialName("key_type") val keyType: String? = null,
    @SerialName("key_value") val keyValue: String? = null
)

@Serializable
internal data class DecodeBrebQrResultWire(
    val amount: JsonElement? = null,
    @SerialName("additional_info") val additionalInfo: JsonElement? = null,
    val inc: JsonElement? = null,
    val key: BrebQrKeyInfo? = null,
    @SerialName("qr_code_data") val qrCodeData: String? = null,
    val status: String? = null,
    @SerialName("acquirer_network_identifier") val acquirerNetworkIdentifier: String? = null,
    val merchant: JsonElement? = null,
    val channel: String? = null,
    val vat: JsonElement? = null,
    @SerialName("qr_code_reference") val qrCodeReference: String? = null,
    val type: String? = null,
    @SerialName("resolution_id") val resolutionId: String? = null,
    val resolution: JsonElement? = null,
    val raw: JsonElement = JsonObject(emptyMap())
)

@Serializable
internal data class DecodeBrebQrResponseWire(
    val result: DecodeBrebQrResultWire,
    @SerialName("req_id") val requestId: String
)

/**
 * Decoded BRE-B EMVCo QR payload. Most fields are free-form (`JsonElement`)
 * since their shape varies by QR type (STATIC vs DYNAMIC) — decode them with
 * `Json.decodeFromJsonElement` when you know the expected shape.
 */
data class BrebDecodedQr constructor(
    val amount: JsonElement? = null,
    val additionalInfo: JsonElement? = null,
    val inc: JsonElement? = null,
    val key: BrebQrKeyInfo? = null,
    val qrCodeData: String? = null,
    val status: String? = null,
    val acquirerNetworkIdentifier: String? = null,
    val merchant: JsonElement? = null,
    val channel: String? = null,
    val vat: JsonElement? = null,
    val qrCodeReference: String? = null,
    /** e.g. "STATIC" or "DYNAMIC" */
    val type: String? = null,
    /** Only present for STATIC QR codes after resolving the embedded key */
    val resolutionId: String? = null,
    /** Only present for STATIC QR codes: the resolved BRE-B recipient data */
    val resolution: JsonElement? = null,
    val raw: JsonElement = JsonObject(emptyMap())
)

data class DeleteBrebKeyResult constructor(
    val deleted: Boolean,
    val accountUrn: String,
    val keyId: String,
    val status: String
)

data class SuspendBrebKeyResult constructor(
    val accountUrn: String,
    val keyId: String,
    val keyStatus: String,
    val status: String
)

data class ActivateBrebKeyResult constructor(
    val accountUrn: String,
    val keyId: String,
    val keyStatus: String,
    val status: String
)

@Serializable
data class BrebKeyInfo(
    @SerialName("key_type") val keyType: BrebKeyType? = null,
    @SerialName("key_value") val keyValue: String
)

@Serializable
data class BrebDetails(
    val id: String,
    @SerialName("remote_key_id") val remoteKeyId: String,
    @SerialName("account_id") val accountId: String,
    val key: BrebKeyInfo,
    @SerialName("display_name") val displayName: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("raw_response") val rawResponse: JsonElement = JsonObject(emptyMap())
)

data class BrebKeyAccount @JvmOverloads constructor(
    val id: String,
    val urn: String,
    val ownerUrn: String,
    val medium: String = "breb",
    val remoteKeyId: String,
    val accountId: String,
    val keyType: BrebKeyType? = null,
    val key: String,
    val displayName: String? = null,
    val status: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val details: BrebDetails,
    val balance: Map<String, TokenBalance>? = null
)

@Serializable
data class BrebResolvedKeyInfo(
    @SerialName("keyType") val keyType: BrebKeyType? = null,
    @SerialName("keyValue") val keyValue: String
)

@Serializable
data class BrebResolvedOwner(
    @SerialName("identificationType") val identificationType: String? = null,
    @SerialName("identificationNumber") val identificationNumber: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("secondName") val secondName: String? = null,
    @SerialName("firstLastName") val firstLastName: String? = null,
    @SerialName("secondLastName") val secondLastName: String? = null,
    val type: String? = null,
    @SerialName("businessName") val businessName: String? = null,
    val name: String? = null
)

@Serializable
data class BrebResolvedParticipant(
    val name: String? = null,
    @SerialName("identificationNumber") val identificationNumber: String? = null
)

@Serializable
data class BrebResolvedAccount(
    @SerialName("accountNumber") val accountNumber: String? = null,
    @SerialName("accountType") val accountType: String? = null
)

@Serializable
data class BrebResolvedKey(
    val id: String,
    @SerialName("resolutionId") val resolutionId: String,
    @SerialName("customerId") val customerId: String,
    val key: BrebResolvedKeyInfo,
    val owner: BrebResolvedOwner? = null,
    val participant: BrebResolvedParticipant? = null,
    val account: BrebResolvedAccount? = null,
    @SerialName("receptorNode") val receptorNode: String? = null,
    @SerialName("resolvedAt") val resolvedAt: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
    val raw: JsonElement = JsonObject(emptyMap())
)

// ============================================
// Transfer Models
// ============================================

/**
 * Supported assets for transfers
 */
enum class SupportedAsset(val value: String) {
    COP_2("COP/2"),
    COPM_2("COPM/2"),
    COPB_6("COPB/6"),
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
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

@Serializable
internal data class TransferRequest(
    @SerialName("destination_account_urn") val destinationAccountUrn: String,
    val amount: String,
    val asset: String,
    val metadata: JsonElement = JsonObject(emptyMap())
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

data class TokenBalance @JvmOverloads constructor(
    val current: String,
    val pending: String,
    val `in`: String? = null,
    val out: String? = null
)

data class GetAccountBalanceParams constructor(
    val urn: String
)

data class GetBalanceParams @JvmOverloads constructor(
    val accountUrns: List<String>? = null
)

data class ListMovementsParams @JvmOverloads constructor(
    val urn: String,
    val asset: String,
    val limit: Int? = null,
    val before: String? = null,
    val after: String? = null,
    val reference: String? = null,
    val direction: String? = null,
    /** When true, collapses related movements (e.g. pending + confirmed) into a single entry showing the latest state */
    val collapsedView: Boolean? = null,
    /** Filter by pocket: 'main' for confirmed movements, 'pending' for pending movements */
    val pocket: String? = null,
    /** Pagination token for fetching next page */
    val next: String? = null
)

@Serializable
data class Movement(
    val amount: String,
    val asset: String,
    @SerialName("from_account_id") val fromAccountId: String,
    @SerialName("to_account_id") val toAccountId: String? = null,
    val direction: String,
    val reference: String,
    @SerialName("rail_name") val railName: String,
    val details: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("created_at") val createdAt: String,
    /** Settlement status: pending, cancelled, confirmed, settled, failed, or ignored */
    val status: String
)

/**
 * Alias for backward compatibility
 */
typealias CardMovement = Movement

/**
 * Parameters for `GET /accounts/transactions` — aggregated cross-account
 * transaction history for all accounts owned by the authenticated user.
 */
data class ListAggregatedTransactionsParams @JvmOverloads constructor(
    val asset: String,
    /** Optional list of specific account URNs to include in aggregation */
    val accountUrns: List<String>? = null,
    val limit: Int? = null,
    val before: String? = null,
    val after: String? = null,
    val reference: String? = null,
    val direction: String? = null,
    /** When true, collapses related movements (e.g. pending + confirmed) into a single entry showing the latest state */
    val collapsedView: Boolean? = null,
    /** Pagination token for fetching next page */
    val next: String? = null
)

/**
 * Paginated response for movements
 */
@Serializable
data class PagedMovements(
    /** Array of movements in this page */
    val data: List<Movement>,
    /** Number of results in this page */
    @SerialName("page_size") val pageSize: Int,
    /** Whether more results are available */
    @SerialName("has_more") val hasMore: Boolean,
    /** Pagination token for next page (if hasMore is true) */
    val next: String? = null
)

// ============================================
// Batch Transfer Models
// ============================================

/**
 * Single operation within a batch transfer
 */
data class BatchTransferOperation @JvmOverloads constructor(
    val fromAccountUrn: String,
    val toAccountUrn: String,
    val reference: String,
    val amount: String,
    val asset: SupportedAsset,
    val metadata: Map<String, Any?>? = null
) {
    companion object {
        /**
         * Create a builder for batch transfer operation (recommended for Java)
         */
        @JvmStatic
        fun builder() = BatchTransferOperationBuilder()
    }
}

/**
 * Builder for BatchTransferOperation (Java-friendly)
 */
class BatchTransferOperationBuilder {
    private var fromAccountUrn: String? = null
    private var toAccountUrn: String? = null
    private var reference: String? = null
    private var amount: String? = null
    private var asset: SupportedAsset? = null
    private var metadata: Map<String, Any?>? = null

    fun fromAccountUrn(urn: String) = apply { this.fromAccountUrn = urn }
    fun toAccountUrn(urn: String) = apply { this.toAccountUrn = urn }
    fun reference(reference: String) = apply { this.reference = reference }
    fun amount(amount: String) = apply { this.amount = amount }
    fun asset(asset: SupportedAsset) = apply { this.asset = asset }
    fun metadata(metadata: Map<String, Any?>) = apply { this.metadata = metadata }

    fun build(): BatchTransferOperation {
        return BatchTransferOperation(
            fromAccountUrn = requireNotNull(fromAccountUrn) { "fromAccountUrn is required" },
            toAccountUrn = requireNotNull(toAccountUrn) { "toAccountUrn is required" },
            reference = requireNotNull(reference) { "reference is required" },
            amount = requireNotNull(amount) { "amount is required" },
            asset = requireNotNull(asset) { "asset is required" },
            metadata = metadata
        )
    }
}

/**
 * Parameters for batch transfer
 */
data class BatchTransferParams @JvmOverloads constructor(
    val operations: List<BatchTransferOperation>,
    val reference: String,
    val metadata: Map<String, Any?>? = null,
    val webhookUrl: String? = null,
    val idempotencyKey: String? = null
) {
    companion object {
        /**
         * Create a builder for batch transfer (recommended for Java)
         */
        @JvmStatic
        fun builder() = BatchTransferBuilder()
    }
}

/**
 * Builder for BatchTransferParams (Java-friendly)
 *
 * Multiple ways to add operations:
 *
 * 1. Direct parameters (simplest):
 * ```kotlin
 * builder.addOperation(
 *     fromAccountUrn = "did:bloque:account:card:usr-xxx:crd-123",
 *     toAccountUrn = "did:bloque:account:card:usr-xxx:crd-456",
 *     reference = "transfer-001",
 *     amount = "1000000",
 *     asset = SupportedAsset.DUSD_6
 * )
 * ```
 *
 * 2. Lambda builder (Kotlin):
 * ```kotlin
 * builder.addOperation {
 *     fromAccountUrn("did:bloque:account:card:usr-xxx:crd-123")
 *     toAccountUrn("did:bloque:account:card:usr-xxx:crd-456")
 *     reference("transfer-001")
 *     amount("1000000")
 *     asset(SupportedAsset.DUSD_6)
 * }
 * ```
 *
 * 3. Pre-built operation:
 * ```kotlin
 * builder.addOperation(operation)
 * ```
 *
 * 4. Set all operations at once:
 * ```kotlin
 * builder.operations(listOfOperations)
 * // or
 * builder.operations(op1, op2, op3)
 * ```
 *
 * Usage (Java):
 * ```java
 * BatchTransferParams params = BatchTransferParams.builder()
 *     .reference("batch-payroll-2024-01-15")
 *     .addOperation(
 *         "did:bloque:account:card:usr-xxx:crd-123",  // from
 *         "did:bloque:account:card:usr-xxx:crd-456",  // to
 *         "transfer-001",                              // reference
 *         "1000000",                                   // amount
 *         SupportedAsset.DUSD_6,                       // asset
 *         null                                         // metadata (optional)
 *     )
 *     .build();
 * ```
 */
class BatchTransferBuilder {
    private val operations = mutableListOf<BatchTransferOperation>()
    private var reference: String? = null
    private var metadata: Map<String, Any?>? = null
    private var webhookUrl: String? = null
    private var idempotencyKey: String? = null

    fun reference(reference: String) = apply { this.reference = reference }
    fun metadata(metadata: Map<String, Any?>) = apply { this.metadata = metadata }
    fun webhookUrl(url: String) = apply { this.webhookUrl = url }
    fun idempotencyKey(key: String) = apply { this.idempotencyKey = key }

    /**
     * Add a pre-built operation to the batch
     */
    fun addOperation(operation: BatchTransferOperation) = apply {
        operations.add(operation)
    }

    /**
     * Add an operation using a builder lambda (Kotlin-friendly)
     */
    fun addOperation(block: BatchTransferOperationBuilder.() -> Unit) = apply {
        operations.add(BatchTransferOperationBuilder().apply(block).build())
    }

    /**
     * Add an operation with all parameters directly (no builder needed)
     *
     * Usage (Kotlin):
     * ```kotlin
     * builder.addOperation(
     *     fromAccountUrn = "did:bloque:account:card:usr-xxx:crd-source",
     *     toAccountUrn = "did:bloque:account:card:usr-xxx:crd-dest",
     *     reference = "transfer-001",
     *     amount = "1000000",
     *     asset = SupportedAsset.DUSD_6
     * )
     * ```
     */
    @JvmOverloads
    fun addOperation(
        fromAccountUrn: String,
        toAccountUrn: String,
        reference: String,
        amount: String,
        asset: SupportedAsset,
        metadata: Map<String, Any?>? = null
    ) = apply {
        operations.add(
            BatchTransferOperation(
                fromAccountUrn = fromAccountUrn,
                toAccountUrn = toAccountUrn,
                reference = reference,
                amount = amount,
                asset = asset,
                metadata = metadata
            )
        )
    }

    /**
     * Add multiple operations at once
     */
    fun addOperations(ops: List<BatchTransferOperation>) = apply {
        operations.addAll(ops)
    }

    /**
     * Add multiple operations at once using vararg
     */
    fun addOperations(vararg ops: BatchTransferOperation) = apply {
        operations.addAll(ops)
    }

    /**
     * Set all operations at once, replacing any existing operations
     */
    fun operations(ops: List<BatchTransferOperation>) = apply {
        operations.clear()
        operations.addAll(ops)
    }

    /**
     * Set all operations at once using vararg, replacing any existing operations
     */
    fun operations(vararg ops: BatchTransferOperation) = apply {
        operations.clear()
        operations.addAll(ops)
    }

    fun build(): BatchTransferParams {
        require(operations.isNotEmpty()) { "At least one operation is required" }
        return BatchTransferParams(
            operations = operations.toList(),
            reference = requireNotNull(reference) { "reference is required" },
            metadata = metadata,
            webhookUrl = webhookUrl,
            idempotencyKey = idempotencyKey
        )
    }
}

// Internal request models for batch transfer

@Serializable
internal data class BatchTransferOperationRequest(
    @SerialName("from_account_urn") val fromAccountUrn: String,
    @SerialName("to_account_urn") val toAccountUrn: String,
    val reference: String,
    val amount: String,
    val asset: String,
    val metadata: JsonElement = JsonObject(emptyMap())
)

@Serializable
internal data class BatchTransferRequest(
    val operations: List<BatchTransferOperationRequest>,
    val reference: String,
    val metadata: JsonElement = JsonObject(emptyMap()),
    @SerialName("webhook_url") val webhookUrl: String? = null
)

@Serializable
internal data class BatchTransferChunk(
    @SerialName("queue_id") val queueId: String,
    val status: String,
    val message: String
)

@Serializable
internal data class BatchTransferResultData(
    /** Outcome of the whole batch submission: "executed", "deferred", or "failed". */
    val status: String,
    val chunks: List<BatchTransferChunk>,
    @SerialName("total_operations") val totalOperations: Int,
    @SerialName("total_chunks") val totalChunks: Int
)

@Serializable
internal data class BatchTransferResponseWrapper(
    val result: BatchTransferResultData,
    @SerialName("req_id") val reqId: String? = null
)

/**
 * Result of a batch transfer chunk
 */
data class BatchTransferChunkResult(
    val queueId: String,
    val status: String,
    val message: String
)

/**
 * Result of a batch transfer operation
 */
data class BatchTransferResult(
    /**
     * Outcome of the batch submission. `"executed"` means all effective
     * operations were queued (including the ready portion of a mixed batch).
     * `"deferred"` means all accounts were still being created and the
     * entire batch was rescheduled. `"failed"` means the batch exhausted all
     * retry attempts.
     */
    val status: String,
    val chunks: List<BatchTransferChunkResult>,
    val totalOperations: Int,
    val totalChunks: Int,
    val reqId: String? = null
)

// ============================================
// Pomelo Card Behavior Metadata (spending control, cashback, fees, MCC)
//
// These are all opt-in configuration blocks written into a card account's
// `metadata` (via CreateCardAccountParams.metadata / UpdateCardMetadataParams)
// — pomelo (the card processor integration) reads them back out of
// `account.metadata` at authorization time. Each config class exposes
// `toMetadataMap()` / a matching top-level helper that produces a
// `Map<String, Any?>` fragment ready to merge into that `metadata` map,
// which then flows through the existing `mapToJsonElement` outbound
// converter already used by every account client.
// ============================================

/** Keys pomelo reads from card account `metadata`. */
object CardMetadataKeys {
    const val SPENDING_CONTROL = "spending_control"
    const val CASHBACK_PROGRAMS = "cashback_programs"
    const val SPENDING_FEES = "spending_fees"
    const val MCC_WHITELIST = "mcc_whitelist"
    const val PRIORITY_MCC = "priority_mcc"
}

/** `metadata.spending_control` mode selector. */
enum class CardSpendingControlMode(val value: String) {
    DEFAULT("default"),
    SMART("smart");

    override fun toString(): String = value
}

/**
 * Card spending-control configuration. Build one of these and merge
 * [toMetadataMap] into the `metadata` passed to `create`/`updateMetadata`.
 *
 * `mccWhitelist` and `priorityMcc` only take effect in [CardSpendingControlMode.SMART]
 * mode — they configure per-pocket MCC routing (see [MccWhitelist]).
 *
 * Example:
 * ```kotlin
 * val config = CardSpendingControlConfig(
 *     mode = CardSpendingControlMode.SMART,
 *     mccWhitelist = mapOf("did:bloque:...:pocket:groceries" to listOf("5411", "5422")),
 *     priorityMcc = listOf("did:bloque:...:pocket:groceries")
 * )
 * session.accounts.card.updateMetadata(
 *     UpdateCardMetadataParams(urn, config.toMetadataMap())
 * )
 * ```
 */
data class CardSpendingControlConfig @JvmOverloads constructor(
    val mode: CardSpendingControlMode,
    /** Maps pocket URN to a whitelist of MCC codes (smart mode only). */
    val mccWhitelist: Map<String, List<String>>? = null,
    /** Pocket URNs in priority order (smart mode only). */
    val priorityMcc: List<String>? = null
) {
    fun toMetadataMap(): Map<String, Any?> = buildMap {
        put(CardMetadataKeys.SPENDING_CONTROL, mode.value)
        mccWhitelist?.let { put(CardMetadataKeys.MCC_WHITELIST, it) }
        priorityMcc?.let { put(CardMetadataKeys.PRIORITY_MCC, it) }
    }
}

/** `mcc_whitelist` value type per pocket. On the wire this may be a URL string
 * or an inline array of MCC codes; the SDK always normalizes to a `List<String>`
 * of inline codes on the way out. */
typealias MccWhitelist = Map<String, List<String>>

/** Builds a `metadata.mcc_whitelist` entry ready to merge into account metadata. */
fun mccWhitelistMetadata(whitelist: MccWhitelist): Map<String, Any?> =
    mapOf(CardMetadataKeys.MCC_WHITELIST to whitelist)

/** `metadata.cashback_programs[].type` */
enum class CashbackProgramType(val value: String) {
    EXTRA_SAVINGS("extra_savings"),
    ROUND_UP("round_up");

    override fun toString(): String = value
}

/** `metadata.cashback_programs[].fee_type` (only meaningful for [CashbackProgramType.EXTRA_SAVINGS]) */
enum class CashbackFeeType(val value: String) {
    PERCENTAGE("percentage"),
    FLAT("flat");

    override fun toString(): String = value
}

/**
 * A single cashback program entry for `metadata.cashback_programs`.
 *
 * - `EXTRA_SAVINGS`: charges an extra percentage or flat amount per transaction,
 *   routed to [targetPocketUrn].
 * - `ROUND_UP`: rounds the transaction up to the next whole unit, routing the
 *   delta to [targetPocketUrn]. [feeType]/[value] are ignored for this type.
 */
data class CashbackProgram @JvmOverloads constructor(
    val programName: String,
    val type: CashbackProgramType,
    val targetPocketUrn: String,
    /** Required for [CashbackProgramType.EXTRA_SAVINGS]. Ignored for ROUND_UP. */
    val feeType: CashbackFeeType? = null,
    /** For percentage: rate (e.g. 0.05 = 5%). For flat: fixed amount in local currency. */
    val value: Double? = null
) {
    fun toMetadataMap(): Map<String, Any?> = buildMap {
        put("program_name", programName)
        put("type", type.value)
        put("target_pocket_urn", targetPocketUrn)
        feeType?.let { put("fee_type", it.value) }
        value?.let { put("value", it) }
    }
}

/** Builds a `metadata.cashback_programs` entry ready to merge into account metadata. */
fun cashbackProgramsMetadata(programs: List<CashbackProgram>): Map<String, Any?> =
    mapOf(CardMetadataKeys.CASHBACK_PROGRAMS to programs.map { it.toMetadataMap() })

/** `metadata.spending_fees[].type` */
enum class SpendingFeeType(val value: String) {
    PERCENTAGE("percentage"),
    FLAT("flat");

    override fun toString(): String = value
}

/** `metadata.spending_fees[].category` — fees categorized "fx" drive the exchange rate spread. */
enum class SpendingFeeCategory(val value: String) {
    FX("fx"),
    INTERCHANGE("interchange"),
    CUSTOM("custom");

    override fun toString(): String = value
}

/**
 * A single spending-fee rule for `metadata.spending_fees` (or origin-level
 * `config.spending_fees`, which is admin-managed and not settable via this SDK).
 *
 * Built-in [rule] names: `fx_conversion`, `amount_range_usd`, `wallet`. When
 * [rule] is absent the fee always applies.
 */
data class SpendingFee @JvmOverloads constructor(
    /** Unique name for the fee (e.g. "bloque-treasury", "fx_fee") */
    val feeName: String,
    /** Target account URN the fee is routed to */
    val accountUrn: String,
    val type: SpendingFeeType,
    /** Rate for percentage (0.0144 = 1.44%) or amount for flat */
    val value: Double,
    /** Defaults to [SpendingFeeCategory.CUSTOM] server-side if omitted */
    val category: SpendingFeeCategory? = null,
    /** Optional rule name that determines when this fee applies */
    val rule: String? = null,
    /** Optional parameters for the rule (e.g. `{"min": 500, "max": 5000}` for amount_range_usd) */
    val ruleParams: Map<String, Any?>? = null
) {
    fun toMetadataMap(): Map<String, Any?> = buildMap {
        put("fee_name", feeName)
        put("account_urn", accountUrn)
        put("type", type.value)
        put("value", value)
        category?.let { put("category", it.value) }
        rule?.let { put("rule", it) }
        ruleParams?.let { put("rule_params", it) }
    }
}

/** Builds a `metadata.spending_fees` entry ready to merge into account metadata. */
fun spendingFeesMetadata(fees: List<SpendingFee>): Map<String, Any?> =
    mapOf(CardMetadataKeys.SPENDING_FEES to fees.map { it.toMetadataMap() })

// ============================================
// Webhook Payload Models
//
// These are typed models for payloads that arrive at the caller's own
// `webhook_url` — there is no SDK client method that produces them. Decode
// the raw request body your webhook receiver gets with, e.g.:
// `Json.decodeFromString<BatchTransferWebhookEvent>(body)`.
//
// No HMAC verification helper exists yet in sdk-core for the
// `x-bloque-signature` header used by these webhooks — verifying the
// signature is left to the caller for now.
// ============================================

/**
 * A batch-transfer lifecycle event delivered to `webhook_url` (see
 * `AccountsClient.batchTransfer`'s `webhookUrl` param). All four event types
 * (`batch_transfer.completed`, `.operations_deferred`, `.retry_attempt`,
 * `.failed`) share this envelope; check [eventType] to know which optional
 * fields are populated. Signed with `x-bloque-signature` (HMAC-SHA256 of the
 * JSON body using the origin's webhook secret) when one is configured.
 */
@Serializable
data class BatchTransferWebhookEvent(
    /** The batch-level reference this event is about. */
    val reference: String,
    @SerialName("event_type") val eventType: String,
    val timestamp: String,

    // -- batch_transfer.completed --
    @SerialName("operation_count") val operationCount: Int? = null,
    @SerialName("queue_ids") val queueIds: List<String>? = null,

    // -- batch_transfer.operations_deferred --
    @SerialName("deferred_reference") val deferredReference: String? = null,
    @SerialName("deferred_operation_count") val deferredOperationCount: Int? = null,
    @SerialName("ready_operation_count") val readyOperationCount: Int? = null,

    // -- batch_transfer.retry_attempt --
    @SerialName("retry_count") val retryCount: Int? = null,
    @SerialName("max_retries") val maxRetries: Int? = null,
    @SerialName("retry_in_seconds") val retryInSeconds: Int? = null,

    // -- shared by operations_deferred / retry_attempt / failed --
    @SerialName("pending_accounts") val pendingAccounts: List<String>? = null,

    // -- batch_transfer.failed --
    val error: String? = null,
    val message: String? = null,
    @SerialName("permanently_failed_accounts") val permanentlyFailedAccounts: List<String>? = null
)

/** `settlement.output.results[]` entry inside [BatchTransferChunkSettlementWebhook]. */
@Serializable
data class BatchTransferSettlementResult(
    val reference: String? = null,
    val operation: String? = null,
    val from: String? = null,
    val to: String? = null,
    val amount: String? = null,
    val status: String? = null,
    val error: String? = null,
    val metadata: JsonElement? = null
)

/** `message` field inside [BatchTransferChunkSettlementWebhook]. */
@Serializable
data class BatchTransferSettlementMessage(
    val urn: String? = null,
    @SerialName("rail_name") val railName: String? = null,
    val metadata: JsonElement? = null
)

/** `settlement.output` field inside [BatchTransferSettlementDetail]. */
@Serializable
data class BatchTransferSettlementOutput(
    val operation: String? = null,
    val results: List<BatchTransferSettlementResult>? = null,
    val reference: String? = null
)

/** `settlement` field inside [BatchTransferChunkSettlementWebhook]. */
@Serializable
data class BatchTransferSettlementDetail(
    val metadata: JsonElement? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("failed_at") val failedAt: String? = null,
    @SerialName("pending_at") val pendingAt: String? = null,
    @SerialName("settled_at") val settledAt: String? = null,
    @SerialName("who_id") val whoId: String? = null,
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("rail_name") val railName: String? = null,
    val status: String? = null,
    val operation: String? = null,
    /** Free-form — the batch call input (shape varies by rail). */
    val input: JsonElement? = null,
    val output: BatchTransferSettlementOutput? = null,
    @SerialName("failed_reason") val failedReason: String? = null
)

/**
 * Per-chunk settlement status webhook forwarded to `webhook_url` for a batch
 * transfer. This is the channel that confirms a transfer actually settled
 * on-chain — `batch_transfer.completed` only confirms it was queued for
 * signing. Signed with the same `x-bloque-signature` HMAC header.
 */
@Serializable
data class BatchTransferChunkSettlementWebhook(
    @SerialName("queue_id") val queueId: String,
    /** "pending" | "confirmed" | "settled" | "failed" */
    val status: String,
    val message: BatchTransferSettlementMessage? = null,
    val settlement: BatchTransferSettlementDetail? = null
)

/**
 * The final, normalized account event envelope delivered to an account's
 * own `webhook_url` — the same shape regardless of which raw provider
 * webhook (Bridge, Cobre/BRE-B, Plaid, EVM hot wallet, ...) produced it.
 * Signed with `x-bloque-signature` when a webhook secret is configured for
 * the caller's origin.
 *
 * [eventData]'s shape depends on [eventType] and [medium] — e.g. for a BRE-B
 * account's `"DEPOSIT"` event, decode it as [BrebDepositEventData].
 */
@Serializable
data class AccountWebhookEvent(
    @SerialName("account_urn") val accountUrn: String,
    val medium: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("event_data") val eventData: JsonElement,
    val timestamp: String
)

/** `sender.owner` inside a BRE-B [BrebDepositEventData]. */
@Serializable
data class BrebDepositSenderOwner(
    val name: String? = null,
    @SerialName("identification_number") val identificationNumber: String? = null,
    @SerialName("identification_type") val identificationType: String? = null
)

/** `sender.account` inside a BRE-B [BrebDepositEventData]. */
@Serializable
data class BrebDepositSenderAccount(
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("account_type") val accountType: String? = null
)

/** `sender.participant` inside a BRE-B [BrebDepositEventData]. */
@Serializable
data class BrebDepositSenderParticipant(
    @SerialName("identification_number") val identificationNumber: String? = null
)

/** `sender` inside a BRE-B [BrebDepositEventData]. */
@Serializable
data class BrebDepositSender(
    val owner: BrebDepositSenderOwner? = null,
    val account: BrebDepositSenderAccount? = null,
    val participant: BrebDepositSenderParticipant? = null
)

/** `amount` inside a BRE-B [BrebDepositEventData]. */
@Serializable
data class BrebDepositAmount(
    val value: String,
    val currency: String
)

/**
 * `event_data` shape for a BRE-B account's normalized `"DEPOSIT"` event, as
 * delivered inside [AccountWebhookEvent.eventData]. Bloque normalizes both
 * Cobre's raw `breb_credit`/`r2p_breb_credit` webhook and legacy Passport's
 * `payment.inbound.settled` webhook to this same shape before forwarding.
 */
@Serializable
data class BrebDepositEventData(
    @SerialName("payment_id") val paymentId: String? = null,
    val reference: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("key_value") val keyValue: String? = null,
    val amount: BrebDepositAmount? = null,
    val sender: BrebDepositSender? = null,
    val status: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val raw: JsonElement? = null,
    /** Present (non-null) only for the DEPOSIT_SWAP_PENDING variant of this event. */
    val message: String? = null
)

// ============================================
// US Account (Bridge) Models
// ============================================

@Serializable
data class UsAccountDetails(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("virtual_account_id") val virtualAccountId: String? = null,
    /** Dynamically generated EVM address for receiving crypto deposits */
    @SerialName("evm_address") val evmAddress: String? = null,
    val currency: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_address") val bankAddress: String? = null,
    @SerialName("bank_routing_number") val bankRoutingNumber: String? = null,
    @SerialName("bank_account_number") val bankAccountNumber: String? = null,
    @SerialName("bank_beneficiary_name") val bankBeneficiaryName: String? = null,
    @SerialName("bank_beneficiary_address") val bankBeneficiaryAddress: String? = null
)

data class UsAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val customerId: String? = null,
    val virtualAccountId: String? = null,
    val evmAddress: String? = null,
    val currency: String? = null,
    val bankName: String? = null,
    val bankAddress: String? = null,
    val bankRoutingNumber: String? = null,
    val bankAccountNumber: String? = null,
    val bankBeneficiaryName: String? = null,
    val bankBeneficiaryAddress: String? = null,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Parameters for creating a `us-account` (Bridge-backed) account.
 *
 * The account holder's name/address/etc. come from the holder identity's own
 * verified profile (mapped server-side via the Bridge profile mapping), not
 * from this call — the only holder-supplied inputs at creation time are the
 * signed Terms of Service agreement and, optionally, a government ID image.
 * Call [UsAccountClient.tosLink] first to obtain [signedAgreementId].
 */
data class CreateUsAccountParams @JvmOverloads constructor(
    /** `signed_agreement_id` returned after the holder accepts ToS via [UsAccountClient.tosLink] */
    val signedAgreementId: String,
    /** Base64-encoded government ID image, if required */
    val govIdImageFront: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class ListUsAccountParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateUsAccountMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

/**
 * Parameters for requesting a Bridge Terms of Service acceptance link
 * (`POST /mediums/us-account/tos-link`), required before creating a us-account.
 */
data class UsAccountTosLinkParams @JvmOverloads constructor(
    /** URI to redirect to after ToS acceptance; the `signed_agreement_id` is passed as a query parameter */
    val redirectUri: String? = null
)

data class UsAccountTosLink constructor(
    /** URL to display to the user (iFrame or new browser window) for ToS acceptance */
    val url: String
)

/** Empty JSON body ("{}") for POST endpoints that take all their input via query params/path. */
@Serializable
internal class EmptyRequestBody

@Serializable
internal data class UsAccountTosLinkResultWire(val url: String)

@Serializable
internal data class UsAccountTosLinkResponseWire(
    val result: UsAccountTosLinkResultWire,
    @SerialName("req_id") val requestId: String? = null
)

// ============================================
// US2 Account (Kira) Models — fiat virtual account, KYC-gated
// ============================================

data class Us2AccountAddress @JvmOverloads constructor(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

/** Bank/wire deposit instructions for the fiat side of a us2-account. */
@Serializable
data class Us2SourceDepositInstructions(
    val currency: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_address") val bankAddress: String? = null,
    @SerialName("bank_routing_number") val bankRoutingNumber: String? = null,
    @SerialName("bank_account_number") val bankAccountNumber: String? = null,
    @SerialName("bank_beneficiary_name") val bankBeneficiaryName: String? = null,
    @SerialName("bank_beneficiary_address") val bankBeneficiaryAddress: String? = null,
    val clabe: String? = null
)

@Serializable
data class Us2AccountDetails(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("virtual_account_id") val virtualAccountId: String? = null,
    /** "US_BANK" or "MX_SPEI" */
    val type: String? = null,
    val currency: String? = null,
    @SerialName("source_deposit_instructions") val sourceDepositInstructions: Us2SourceDepositInstructions? = null
)

data class Us2Account @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val userId: String? = null,
    val virtualAccountId: String? = null,
    val type: String? = null,
    val currency: String? = null,
    val sourceDepositInstructions: Us2SourceDepositInstructions? = null,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Parameters for creating a `us2-account` (fiat virtual account, KYC-gated,
 * dynamic EVM deposit wallet, swap-backed settlement).
 */
data class CreateUs2AccountParams @JvmOverloads constructor(
    /** "individual" or "business" */
    val type: String,
    val email: String,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val companyName: String? = null,
    val incorporationDate: String? = null,
    val ein: String? = null,
    val phone: String? = null,
    val address: Us2AccountAddress? = null,
    val taxId: String? = null,
    /** Base64-encoded proof of address document */
    val proofOfAddress: String? = null,
    /** Required for business accounts */
    val businessFormationDocument: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class ListUs2AccountParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateUs2AccountMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

// ============================================
// External US Bank (Brale/Plaid) Models
// ============================================

@Serializable
data class ExternalUsBankBankAddress(
    @SerialName("street_line_1") val streetLine1: String? = null,
    @SerialName("street_line_2") val streetLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val country: String? = null
)

@Serializable
data class ExternalUsBankDetails(
    /** Stable medium-side account id (UUID), kept for the account lifetime */
    val id: String? = null,
    /** "pending_link" | "active" | "link_failed" | "closed" */
    @SerialName("link_status") val linkStatus: String? = null,
    @SerialName("brale_account_id") val braleAccountId: String? = null,
    @SerialName("brale_address_id") val braleAddressId: String? = null,
    @SerialName("bank_account_last4") val bankAccountLast4: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    // -- pending_link fields --
    @SerialName("link_token") val linkToken: String? = null,
    @SerialName("link_token_expiration") val linkTokenExpiration: String? = null,
    @SerialName("link_url") val linkUrl: String? = null,
    /** Short-lived plaid-link JWT — pass to [ExternalUsBankClient.completePlaidLink] */
    val jwt: String? = null,
    // -- link_failed field --
    @SerialName("failure_reason") val failureReason: String? = null,
    // -- active (enrichment) fields, sourced from Brale address details --
    val owner: String? = null,
    @SerialName("routing_number") val routingNumber: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    /** "checking" or "savings" */
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("bank_address") val bankAddress: ExternalUsBankBankAddress? = null,
    @SerialName("beneficiary_address") val beneficiaryAddress: ExternalUsBankBankAddress? = null,
    @SerialName("transfer_types") val transferTypes: List<String>? = null,
    @SerialName("needs_update") val needsUpdate: Boolean? = null,
    @SerialName("last_updated") val lastUpdated: String? = null
)

data class ExternalUsBankAccount @JvmOverloads constructor(
    val urn: String,
    val id: String,
    val linkStatus: String? = null,
    val braleAccountId: String? = null,
    val braleAddressId: String? = null,
    val bankAccountLast4: String? = null,
    val bankName: String? = null,
    val linkToken: String? = null,
    val linkTokenExpiration: String? = null,
    val linkUrl: String? = null,
    val jwt: String? = null,
    val failureReason: String? = null,
    val owner: String? = null,
    val routingNumber: String? = null,
    val accountNumber: String? = null,
    val accountType: String? = null,
    val bankAddress: ExternalUsBankBankAddress? = null,
    val beneficiaryAddress: ExternalUsBankBankAddress? = null,
    val transferTypes: List<String>? = null,
    val needsUpdate: Boolean? = null,
    val lastUpdated: String? = null,
    val status: String,
    val ownerUrn: String,
    val ledgerId: String? = null,
    val webhookUrl: String? = null,
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Parameters for linking an external US bank account (Brale/Plaid).
 *
 * `create()` only starts the linkage — it returns a `link_token`/`link_url`
 * (see [ExternalUsBankAccount.linkUrl] and [ExternalUsBankClient.plaidLinkUrl])
 * for the holder to complete Plaid Link, then [ExternalUsBankClient.completePlaidLink]
 * exchanges the resulting `public_token`.
 */
data class CreateExternalUsBankAccountParams @JvmOverloads constructor(
    /** Caller-provided label (defaults to "Bank account" server-side) */
    val label: String? = null,
    /** URL the user is redirected to after Plaid Link finishes; must be allowlisted for the origin */
    val returnUrl: String? = null,
    /** Opaque state forwarded through to [returnUrl] (max 256 characters) */
    val state: String? = null,
    val holderUrn: String? = null,
    val webhookUrl: String? = null,
    val ledgerId: String? = null,
    val metadata: Map<String, Any?>? = null,
    val idempotencyKey: String? = null
)

data class ListExternalUsBankParams @JvmOverloads constructor(
    val urn: String? = null,
    val holderUrn: String? = null,
    val status: String? = null
)

data class UpdateExternalUsBankMetadataParams constructor(
    val urn: String,
    val metadata: Map<String, Any?>
)

/**
 * Parameters for proactively pulling funds from a linked US bank via Brale
 * `ach_debit`, auto-swapped to DUSD on Kusama Asset Hub.
 */
data class PullExternalUsBankParams @JvmOverloads constructor(
    val accountUrn: String,
    /** USD decimal amount to pull (e.g. "100.00") */
    val amount: String,
    val idempotencyKey: String? = null
)

@Serializable
internal data class PullExternalUsBankRequest(
    val amount: String,
    @SerialName("idempotency_key") val idempotencyKey: String? = null
)

data class PullExternalUsBankResult constructor(
    /** Order signature emitted by swap.take */
    val orderSig: String? = null,
    val graphId: String? = null,
    val status: String? = null,
    /** Result of the auto-executed first node (Brale resolver), when present */
    val execution: JsonElement? = null
)

@Serializable
internal data class PullExternalUsBankResponseWire(
    val result: PullExternalUsBankResultWire? = null,
    @SerialName("req_id") val requestId: String? = null
)

@Serializable
internal data class PullExternalUsBankResultWire(
    @SerialName("order_sig") val orderSig: String? = null,
    @SerialName("graph_id") val graphId: String? = null,
    val status: String? = null,
    val execution: JsonElement? = null
)

/**
 * Builds the browser URL for the Plaid Link hosted page
 * (`GET /mediums/external-us-bank/plaid-link`) — a browser/webview-only HTML
 * page, not a JSON API. Open it in an iFrame/webview; it drives Plaid Link
 * and then calls [ExternalUsBankClient.completePlaidLink] itself, or (if
 * [returnUrl] was set) redirects there with [state] and the resulting
 * `public_token`/`account_urn`.
 */
data class PlaidLinkUrlParams @JvmOverloads constructor(
    val accountUrn: String,
    /** From [ExternalUsBankAccount.linkToken] */
    val linkToken: String,
    /** From [ExternalUsBankAccount.jwt] */
    val jwt: String,
    val returnUrl: String? = null,
    val state: String? = null
)

/**
 * Parameters for completing Plaid Link onboarding
 * (`POST /mediums/external-us-bank/complete`).
 */
data class CompletePlaidLinkParams constructor(
    val accountUrn: String,
    /** Plaid `public_token` returned by Plaid Link on the frontend */
    val publicToken: String,
    /** Short-lived plaid-link JWT from [ExternalUsBankAccount.jwt] — sent as the request's bearer token */
    val jwt: String
)

@Serializable
internal data class CompletePlaidLinkRequest(
    @SerialName("account_urn") val accountUrn: String,
    @SerialName("public_token") val publicToken: String
)
