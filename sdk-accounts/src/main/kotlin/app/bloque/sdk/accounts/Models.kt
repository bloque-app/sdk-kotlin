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
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
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
    val createdAt: String = "",
    val updatedAt: String = ""
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
    val createdAt: String = "",
    val updatedAt: String = ""
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
    val metadata: JsonElement = JsonObject(emptyMap()),
    val createdAt: String = "",
    val updatedAt: String = ""
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
    val chunks: List<BatchTransferChunkResult>,
    val totalOperations: Int,
    val totalChunks: Int,
    val reqId: String? = null
)
