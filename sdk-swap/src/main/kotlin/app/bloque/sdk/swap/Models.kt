package app.bloque.sdk.swap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================
// Find Rates - Request Models
// ============================================

data class FindRatesParams @JvmOverloads constructor(
    val fromAsset: String,
    val toAsset: String,
    val fromMediums: List<String>,
    val toMediums: List<String>,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val sort: SortOrder? = null,
    val sortBy: SortBy? = null
)

enum class SortOrder(val value: String) {
    ASC("asc"),
    DESC("desc");

    companion object {
        fun fromString(value: String): SortOrder {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown sort order: $value")
        }
    }
}

enum class SortBy(val value: String) {
    RATE("rate"),
    AT("at");

    companion object {
        fun fromString(value: String): SortBy {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown sort by: $value")
        }
    }
}

// ============================================
// Find Rates - Response Models
// ============================================

data class FindRatesResult(
    val rates: List<SwapRate>
)

data class SwapRate(
    val id: String,
    val sig: String,
    val swapSig: String,
    val maker: String,
    val edge: Pair<String, String>,
    val fee: Fee,
    val at: String,
    val until: String,
    val fromMediums: List<String>,
    val toMediums: List<String>,
    val rate: Pair<Double, Double>,
    val ratio: Double,
    val fromLimits: Pair<String, String>,
    val toLimits: Pair<String, String>,
    val createdAt: String,
    val updatedAt: String
)

data class Fee(
    val at: Long,
    val value: Double,
    val formula: String,
    val components: List<FeeComponent>
)

data class FeeComponent @JvmOverloads constructor(
    val at: Long,
    val name: String,
    val type: FeeComponentType,
    val value: Double,
    val percentage: Double? = null,
    val pair: String? = null,
    val amount: Double? = null,
    /**
     * Pins this fee component to a specific oracle's rate row rather than
     * the latest unexpired row for the pair. Only meaningful when
     * [type] is [FeeComponentType.RATE]. Null means "latest unexpired row".
     */
    val serviceName: String? = null
)

enum class FeeComponentType(val value: String) {
    PERCENTAGE("percentage"),
    RATE("rate"),
    FIXED("fixed");

    companion object {
        fun fromString(value: String): FeeComponentType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown fee component type: $value")
        }
    }
}

// ============================================
// PSE Banks - Response Models
// ============================================

data class ListBanksResult(
    val banks: List<Bank>
)

data class Bank(
    val code: String,
    val name: String
)

// ============================================
// Order Webhook Payloads
//
// Public (non-internal) @Serializable models: these describe the JSON body
// POSTed to `webhookUrl` on any *OrderParams, for consumers to deserialize
// themselves (e.g. `Json.decodeFromString<OrderWebhookEvent>(requestBody)`).
// ============================================

/**
 * Event name constants for [OrderWebhookEvent.event].
 */
object OrderWebhookEventName {
    /** Terminal-only: fired once when the order reaches "completed" or "failed". */
    const val STATUS_UPDATED = "order.status.updated"

    /**
     * Non-terminal: fired when Brale's `payment.completed` settles the ACH
     * debit funding a US-bank order, ahead of the order's own terminal
     * status (tracked separately via the underlying transfer). Carries
     * [OrderWebhookEvent.payment].
     */
    const val PAYMENT_ON_ACH_RAIL = "order.payment_on_ach_rail"
}

/**
 * Brale transfer amount attached to [OrderWebhookPayment].
 */
@Serializable
data class OrderWebhookPaymentAmount(
    val value: String,
    val currency: String
)

/**
 * Brale payment data attached to `order.payment_on_ach_rail` notifications.
 * Passed through mostly as-is from Brale's `payment.completed` event.
 */
@Serializable
data class OrderWebhookPayment(
    /** Brale transfer id (per Brale, `data.id` on `payment.completed` is the transfer id). */
    val id: String,
    val amount: OrderWebhookPaymentAmount,
    val direction: String,
    val type: String
)

/**
 * Order snapshot embedded in every order webhook event. All amounts are
 * strings (raw bigint columns serialized as strings).
 */
@Serializable
data class SerializedSwapOrder(
    @SerialName("order_sig") val orderSig: String,
    @SerialName("swap_sig") val swapSig: String,
    @SerialName("rate_sig") val rateSig: String,
    val maker: String,
    val taker: String,
    @SerialName("from_medium") val fromMedium: String,
    @SerialName("from_asset") val fromAsset: String,
    @SerialName("from_amount") val fromAmount: String,
    @SerialName("to_medium") val toMedium: String,
    @SerialName("to_asset") val toAsset: String,
    @SerialName("to_amount") val toAmount: String,
    val status: String,
    val at: Long,
    @SerialName("graph_id") val graphId: String,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("failure_details") val failureDetails: Map<String, String>? = null
)

/**
 * Envelope for both order webhook event types:
 *  - [OrderWebhookEventName.STATUS_UPDATED] (terminal-only; `previousStatus`
 *    set, `payment` absent).
 *  - [OrderWebhookEventName.PAYMENT_ON_ACH_RAIL] (non-terminal; `payment` set,
 *    `previousStatus`/`error` absent).
 */
@Serializable
data class OrderWebhookEvent(
    val event: String,
    val order: SerializedSwapOrder,
    val timestamp: String,
    @SerialName("previous_status") val previousStatus: String? = null,
    val error: String? = null,
    /** Present only on [OrderWebhookEventName.PAYMENT_ON_ACH_RAIL] events. */
    val payment: OrderWebhookPayment? = null
)

// ============================================
// Wire Models (Internal - API Response Format)
// ============================================

@Serializable
internal data class FindRatesResponseWire(
    val rates: List<RateWire>
)

@Serializable
internal data class RateWire(
    val id: String,
    val sig: String,
    @SerialName("swap_sig") val swapSig: String,
    val maker: String,
    val edge: List<String>,
    val fee: FeeWire,
    val at: String,
    val until: String,
    @SerialName("from_medium") val fromMedium: List<String>,
    @SerialName("to_medium") val toMedium: List<String>,
    val rate: List<Double>,
    val ratio: Double,
    @SerialName("from_limits") val fromLimits: List<String>,
    @SerialName("to_limits") val toLimits: List<String>,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class FeeWire(
    val at: Long,
    val value: Double,
    val formula: String,
    val components: List<FeeComponentWire>
)

@Serializable
internal data class FeeComponentWire(
    val at: Long,
    val name: String,
    val type: String,
    val value: Double,
    val percentage: Double? = null,
    val pair: String? = null,
    val amount: Double? = null,
    @SerialName("service_name") val serviceName: String? = null
)

@Serializable
internal data class ListPseBanksResponseWire(
    val banks: List<PseBankWire>
)

@Serializable
internal data class PseBankWire(
    @SerialName("financial_institution_code") val financialInstitutionCode: String,
    @SerialName("financial_institution_name") val financialInstitutionName: String
)

// ============================================
// PSE Create Order - Request Models
// ============================================

enum class OrderType(val value: String) {
    SRC("src"),
    DST("dst")
}

/**
 * Order status for filtering
 */
enum class OrderStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        @JvmStatic
        fun fromString(value: String): OrderStatus {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown order status: $value. Valid values: pending, in_progress, completed, failed")
        }
    }
}

// ============================================
// List Orders - Request/Response Models
// ============================================

/**
 * Parameters for listing orders as taker
 */
data class ListOrdersParams @JvmOverloads constructor(
    /** Filter by order status */
    val status: OrderStatus? = null,
    /** Filter by maker URN */
    val makerUrn: String? = null,
    /** Filter by order signature */
    val orderSig: String? = null,
    /** Filter by swap signature */
    val swapSig: String? = null,
    /** Filter by rate signature */
    val rateSig: String? = null,
    /** Filter by graph ID */
    val graphId: String? = null,
    /** Filter orders created after this timestamp (Unix milliseconds) */
    val after: Long? = null,
    /** Filter orders created before this timestamp (Unix milliseconds) */
    val before: Long? = null
)

/**
 * Result of listing orders
 */
data class ListOrdersResult(
    val orders: List<SwapOrder>
)

data class DepositInformation(
    val urn: String
)

data class CustomerData(
    val fullName: String,
    val phoneNumber: String
)

data class PseOrderArgs @JvmOverloads constructor(
    val bankCode: String,
    val userType: String? = null,
    val customerEmail: String? = null,
    val userLegalIdType: String? = null,
    val userLegalId: String? = null,
    val customerData: CustomerData? = null
)

data class CreatePseOrderParams @JvmOverloads constructor(
    val rateSig: String,
    val toMedium: String,
    val depositInformation: DepositInformation,
    /**
     * The URL the payer's bank redirects them back to once the PSE payment
     * flow ends (approved, declined, or abandoned). PSE is a bank-redirect
     * rail — this is a hard API requirement with no default or placeholder;
     * orders missing it are rejected before the payment gateway is ever
     * contacted. Sent on the request as both `args.redirect_url` (read by
     * `ModulePSE` when the node auto-executes) and `metadata.redirect_url`
     * (read by templates that source it from the persisted order metadata).
     */
    val redirectUrl: String,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val args: PseOrderArgs? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null,
    /** Webhook URL to receive order status update notifications */
    val webhookUrl: String? = null
)

// ============================================
// PSE Create Order - Response Models
// ============================================

data class CreatePseOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// BRE-B Payout Order - Request Models
// ============================================

data class BrebDepositInformation(
    val resolutionId: String
)

data class BrebOrderArgs(
    val sourceAccountUrn: String
)

data class CreateBrebOrderParams @JvmOverloads constructor(
    val rateSig: String,
    val depositInformation: BrebDepositInformation,
    val args: BrebOrderArgs,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null,
    val webhookUrl: String? = null
) {
    companion object {
        @JvmStatic
        fun builder() = CreateBrebOrderParamsBuilder()
    }
}

class CreateBrebOrderParamsBuilder {
    private var rateSig: String? = null
    private var depositInformation: BrebDepositInformation? = null
    private var args: BrebOrderArgs? = null
    private var amountSrc: String? = null
    private var amountDst: String? = null
    private var type: OrderType? = null
    private var nodeId: String? = null
    private var metadata: Map<String, String>? = null
    private var idempotencyKey: String? = null
    private var webhookUrl: String? = null

    fun rateSig(rateSig: String) = apply { this.rateSig = rateSig }
    fun depositInformation(info: BrebDepositInformation) = apply { this.depositInformation = info }
    fun resolutionId(resolutionId: String) = apply {
        this.depositInformation = BrebDepositInformation(resolutionId)
    }
    fun args(args: BrebOrderArgs) = apply { this.args = args }
    fun sourceAccountUrn(urn: String) = apply { this.args = BrebOrderArgs(urn) }
    fun amountSrc(amount: String) = apply { this.amountSrc = amount }
    fun amountDst(amount: String) = apply { this.amountDst = amount }
    fun type(type: OrderType) = apply { this.type = type }
    fun nodeId(nodeId: String) = apply { this.nodeId = nodeId }
    fun metadata(metadata: Map<String, String>) = apply { this.metadata = metadata }
    fun idempotencyKey(key: String) = apply { this.idempotencyKey = key }
    fun webhookUrl(url: String) = apply { this.webhookUrl = url }

    fun build(): CreateBrebOrderParams {
        return CreateBrebOrderParams(
            rateSig = requireNotNull(rateSig) { "rateSig is required" },
            depositInformation = requireNotNull(depositInformation) { "depositInformation is required" },
            args = requireNotNull(args) { "args is required" },
            amountSrc = amountSrc,
            amountDst = amountDst,
            type = type,
            nodeId = nodeId,
            metadata = metadata,
            idempotencyKey = idempotencyKey,
            webhookUrl = webhookUrl
        )
    }
}

data class CreateBrebOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// BRE-B Payout Order - Wire Models
// ============================================

@Serializable
internal data class BrebOrderArgsWire(
    @SerialName("account_urn") val accountUrn: String
)

@Serializable
internal data class BrebDepositInformationWire(
    @SerialName("resolution_id") val resolutionId: String
)

@Serializable
internal data class CreateBrebOrderInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String = "kusama",
    @SerialName("to_medium") val toMedium: String = "breb",
    @SerialName("deposit_information") val depositInformation: BrebDepositInformationWire,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    val args: BrebOrderArgsWire,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)

// ============================================
// BRE-B Deposit Order (cash-in: BRE-B COP -> Kusama) - Request Models
// ============================================

/**
 * Parameters for creating a BRE-B deposit (cash-in) order: BRE-B COP -> Kusama.
 *
 * Unlike [CreateBrebOrderParams] (the payout/cash-out direction), this
 * direction needs no client-supplied execution `args` — the underlying
 * `breb-deposit` module registers a temporary deposit key and derives every
 * argument it needs (`expected_owner_urn`, `reference`, `amount`) from the
 * order itself; only the receiving ledger account is caller-supplied, via
 * [depositInformation].
 */
data class CreateBrebDepositOrderParams @JvmOverloads constructor(
    val rateSig: String,
    /** Ledger account URN that will receive the deposited funds. */
    val depositInformation: DepositInformation,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null,
    val webhookUrl: String? = null
)

data class CreateBrebDepositOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// BRE-B Deposit Order - Wire Models
// ============================================

@Serializable
internal data class CreateBrebDepositOrderInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String = "breb",
    @SerialName("to_medium") val toMedium: String = "kusama",
    @SerialName("deposit_information") val depositInformation: DepositInformationWire,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)

data class SwapOrder(
    val id: String,
    val orderSig: String,
    val rateSig: String,
    val swapSig: String,
    val taker: String,
    val maker: String,
    val fromAsset: String,
    val toAsset: String,
    val fromMedium: String,
    val toMedium: String,
    val fromAmount: String,
    val toAmount: String,
    val at: String,
    val graphId: String,
    val status: String,
    val metadata: Map<String, String>?,
    val createdAt: String,
    val updatedAt: String
)

data class ExecutionResult(
    val nodeId: String,
    val result: ExecutionResultDetails
)

data class ExecutionResultDetails(
    val status: String,
    val name: String?,
    val description: String?,
    val how: ExecutionHow?,
    val callbackToken: String?
)

data class ExecutionHow(
    val url: String?
)

// ============================================
// PSE Create Order - Wire Models (Internal)
// ============================================

@Serializable
internal data class CreateOrderInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String,
    @SerialName("to_medium") val toMedium: String,
    @SerialName("deposit_information") val depositInformation: DepositInformationWire,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    val args: PseOrderArgsWire? = null,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)

@Serializable
internal data class DepositInformationWire(
    val urn: String
)

@Serializable
internal data class CustomerDataWire(
    @SerialName("full_name") val fullName: String,
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
internal data class PseOrderArgsWire(
    @SerialName("bank_code") val bankCode: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("user_legal_id_type") val userLegalIdType: String? = null,
    @SerialName("user_legal_id") val userLegalId: String? = null,
    @SerialName("customer_data") val customerData: CustomerDataWire? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null
)

@Serializable
internal data class CreateOrderResponseWire(
    val result: CreateOrderResultWire,
    @SerialName("req_id") val reqId: String
)

@Serializable
internal data class ListOrdersResponseWire(
    val orders: List<OrderWire>
)

@Serializable
internal data class CreateOrderResultWire(
    val order: OrderWire,
    val execution: ExecutionResultWire? = null
)

@Serializable
internal data class OrderWire(
    val id: String,
    @SerialName("order_sig") val orderSig: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("swap_sig") val swapSig: String,
    val taker: String,
    val maker: String,
    @SerialName("from_asset") val fromAsset: String,
    @SerialName("to_asset") val toAsset: String,
    @SerialName("from_medium") val fromMedium: String,
    @SerialName("to_medium") val toMedium: String,
    @SerialName("from_amount") val fromAmount: String,
    @SerialName("to_amount") val toAmount: String,
    val at: String,
    @SerialName("graph_id") val graphId: String,
    val status: String,
    val metadata: Map<String, String>? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class ExecutionResultWire(
    @SerialName("node_id") val nodeId: String,
    val result: ExecutionResultDetailsWire
)

@Serializable
internal data class ExecutionResultDetailsWire(
    val status: String,
    val name: String? = null,
    val description: String? = null,
    val how: ExecutionHowWire? = null,
    @SerialName("callback_token") val callbackToken: String? = null
)

@Serializable
internal data class ExecutionHowWire(
    val url: String? = null
)

// ============================================
// Cancel Subscription - Request/Response Models
// ============================================

/**
 * Result status of a cancel-subscription call.
 */
enum class CancelSubscriptionStatus(val value: String) {
    /** Cancellation flag was just set; future occurrences will short-circuit. */
    CANCELLATION_PENDING("cancellation_pending"),
    /** The flag was already set by a previous call (idempotent no-op). */
    ALREADY_CANCELLED("already_cancelled"),
    /** The graph has already terminated (success or failure); nothing to cancel. */
    GRAPH_DONE("graph_done");

    companion object {
        @JvmStatic
        fun fromString(value: String): CancelSubscriptionStatus {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException(
                    "Unknown cancel-subscription status: $value. Valid values: cancellation_pending, already_cancelled, graph_done"
                )
        }
    }
}

data class CancelSubscriptionResult(
    val status: CancelSubscriptionStatus,
    /** Index of the next-to-fire occurrence, or null if every tick has already resolved. */
    val cursor: Int?,
    val orderId: String,
    val graphId: String
)

@Serializable
internal data class CancelSubscriptionResponseWire(
    val result: CancelSubscriptionResultWire,
    @SerialName("req_id") val reqId: String
)

@Serializable
internal data class CancelSubscriptionResultWire(
    val status: String,
    val cursor: Int? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("graph_id") val graphId: String
)

// ============================================
// Recurring Card Subscription (recurring-card -> kusama) - Request Models
// ============================================

/**
 * Unit for a schedule allocation slice — see [RecurringCardAllocation].
 */
enum class AllocationUnit(val value: String) {
    PERCENT("percent"),
    CENTS("cents");

    companion object {
        @JvmStatic
        fun fromString(value: String): AllocationUnit {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown allocation unit: $value. Valid values: percent, cents")
        }
    }
}

/**
 * One charge slice in a recurring-card schedule. Either a percentage of the
 * order's total amount or a fixed cents amount, applied per occurrence in
 * the order the allocations are given (e.g. a trial-then-full-price schedule).
 */
data class RecurringCardAllocation @JvmOverloads constructor(
    val unit: AllocationUnit,
    val value: Double,
    val label: String? = null
)

/**
 * Opaque internal ledger transfer instruction, forwarded verbatim to the
 * payout chain. See [RecurringCardDepositInformation.paymentTransfers].
 */
data class PaymentTransfer @JvmOverloads constructor(
    val creditAccountId: String,
    val amount: String,
    val reference: String,
    val metadata: Map<String, String>? = null
)

/**
 * Opaque on-chain withdrawal instruction, forwarded verbatim to the payout
 * chain. See [RecurringCardDepositInformation.paymentWithdrawals].
 */
data class PaymentWithdrawal(
    val coin: String,
    val address: String,
    val network: String,
    val amount: String,
    val reference: String
)

/**
 * Persisted (card-free) schedule details for a recurring-card subscription.
 * Card/3DS/customer data are deliberately NOT part of this — they are
 * supplied only via the transient [CreateCardSubscriptionParams.args] at
 * signup and are never persisted.
 */
data class RecurringCardDepositInformation @JvmOverloads constructor(
    /** Cron expression driving occurrence due dates. */
    val cron: String,
    /** Non-empty list of charge slices making up the schedule. */
    val allocations: List<RecurringCardAllocation>,
    val customerEmail: String,
    val startDate: String? = null,
    val trialDays: Int? = null,
    val endDate: String? = null,
    val timezone: String? = null,
    /** Hard horizon on the number of occurrences; defaults server-side to 24 if neither this nor endDate is set. */
    val maxOccurrences: Int? = null,
    /** Customer device IP captured at signup; forwarded to the card processor on each charge. */
    val ip: String? = null,
    val paymentTransfers: List<PaymentTransfer>? = null,
    val paymentWithdrawals: List<PaymentWithdrawal>? = null
)

/**
 * Card details used to tokenize a new payment source. Mutually exclusive
 * with supplying [TokenizeCardArgs.cardToken] on the enclosing args.
 */
data class CardDetails(
    val number: String,
    val cvc: String,
    val expMonth: String,
    val expYear: String,
    val cardHolder: String
)

data class CardCustomerData @JvmOverloads constructor(
    val phoneNumber: String? = null,
    val fullName: String? = null
)

/**
 * Browser fingerprint forwarded for 3DS challenge flows.
 */
data class CardBrowserInfo(
    val browserColorDepth: String,
    val browserScreenHeight: String,
    val browserScreenWidth: String,
    val browserLanguage: String,
    val browserUserAgent: String,
    val browserTz: String
)

/**
 * Transient tokenization arguments supplied only at signup (`PUT /order`'s
 * top-level `args`), never persisted to `deposit_information`. Exactly one
 * of [card] or [cardToken] must be provided.
 *
 * Set [isThreeDs] = true for unattended recurring charges (3RI) — required
 * on most issuers for a card to be chargeable without the cardholder present
 * on subsequent occurrences.
 */
data class TokenizeCardArgs @JvmOverloads constructor(
    val customerEmail: String,
    val card: CardDetails? = null,
    val cardToken: String? = null,
    val isThreeDs: Boolean? = null,
    val browserInfo: CardBrowserInfo? = null,
    val threeDsAuthType: String? = null,
    val customerData: CardCustomerData? = null
) {
    init {
        require((card == null) != (cardToken == null)) {
            "Exactly one of card or cardToken must be provided"
        }
    }
}

data class CreateCardSubscriptionParams @JvmOverloads constructor(
    val rateSig: String,
    val depositInformation: RecurringCardDepositInformation,
    val args: TokenizeCardArgs,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null,
    val webhookUrl: String? = null
)

data class CreateCardSubscriptionResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// Recurring Card Subscription - Wire Models (Internal)
// ============================================

@Serializable
internal data class RecurringCardAllocationWire(
    val unit: String,
    val value: Double,
    val label: String? = null
)

@Serializable
internal data class PaymentTransferWire(
    @SerialName("credit_account_id") val creditAccountId: String,
    val amount: String,
    val reference: String,
    val metadata: Map<String, String>? = null
)

@Serializable
internal data class PaymentWithdrawalWire(
    val coin: String,
    val address: String,
    val network: String,
    val amount: String,
    val reference: String
)

@Serializable
internal data class RecurringCardDepositInformationWire(
    val cron: String,
    val allocations: List<RecurringCardAllocationWire>,
    @SerialName("customer_email") val customerEmail: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("trial_days") val trialDays: Int? = null,
    @SerialName("end_date") val endDate: String? = null,
    val timezone: String? = null,
    @SerialName("max_occurrences") val maxOccurrences: Int? = null,
    val ip: String? = null,
    @SerialName("payment_transfers") val paymentTransfers: List<PaymentTransferWire>? = null,
    @SerialName("payment_withdrawals") val paymentWithdrawals: List<PaymentWithdrawalWire>? = null
)

@Serializable
internal data class CardDetailsWire(
    val number: String,
    val cvc: String,
    @SerialName("exp_month") val expMonth: String,
    @SerialName("exp_year") val expYear: String,
    @SerialName("card_holder") val cardHolder: String
)

@Serializable
internal data class CardCustomerDataWire(
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
internal data class CardBrowserInfoWire(
    @SerialName("browser_color_depth") val browserColorDepth: String,
    @SerialName("browser_screen_height") val browserScreenHeight: String,
    @SerialName("browser_screen_width") val browserScreenWidth: String,
    @SerialName("browser_language") val browserLanguage: String,
    @SerialName("browser_user_agent") val browserUserAgent: String,
    @SerialName("browser_tz") val browserTz: String
)

@Serializable
internal data class TokenizeCardArgsWire(
    @SerialName("customer_email") val customerEmail: String,
    val card: CardDetailsWire? = null,
    @SerialName("card_token") val cardToken: String? = null,
    @SerialName("is_three_ds") val isThreeDs: Boolean? = null,
    @SerialName("browser_info") val browserInfo: CardBrowserInfoWire? = null,
    @SerialName("three_ds_auth_type") val threeDsAuthType: String? = null,
    @SerialName("customer_data") val customerData: CardCustomerDataWire? = null
)

@Serializable
internal data class CreateCardSubscriptionInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String = "recurring-card",
    @SerialName("to_medium") val toMedium: String = "kusama",
    @SerialName("deposit_information") val depositInformation: RecurringCardDepositInformationWire,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    val args: TokenizeCardArgsWire,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)

// ============================================
// ColBank (Colombian Bank Withdrawal) - Request Models
// ============================================

/**
 * Bank account type for Colombian bank withdrawals
 */
enum class BankAccountType(val value: String) {
    SAVINGS("savings"),
    CHECKINGS("checkings");

    companion object {
        @JvmStatic
        fun fromString(value: String): BankAccountType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown bank account type: $value. Valid values: savings, checkings")
        }
    }
}

/**
 * Identification type for Colombian bank account holder
 */
enum class IdentificationType(val value: String) {
    CC("CC"),           // Cédula de Ciudadanía
    CE("CE"),           // Cédula de Extranjería
    NIT("NIT"),         // NIT (for businesses)
    PASSPORT("PASSPORT");  // Passport

    companion object {
        @JvmStatic
        fun fromString(value: String): IdentificationType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown identification type: $value. Valid values: CC, CE, NIT, PASSPORT")
        }
    }
}

/**
 * Arguments for ColBank order execution
 */
data class ColBankOrderArgs(
    /** Account URN to withdraw from */
    val accountUrn: String
)

/**
 * Bank account deposit information for Colombian bank withdrawal
 */
data class ColBankDepositInformation(
    /** Type of bank account (savings or checking) */
    val bankAccountType: BankAccountType,
    /** Bank account number */
    val bankAccountNumber: String,
    /** Full name of the account holder */
    val bankAccountHolderName: String,
    /** Identification type (CC, CE, NIT, PP) */
    val bankAccountHolderIdentificationType: IdentificationType,
    /** Identification number/value */
    val bankAccountHolderIdentificationValue: String
) {
    companion object {
        @JvmStatic
        fun builder() = ColBankDepositInformationBuilder()
    }
}

/**
 * Builder for ColBankDepositInformation (Java-friendly)
 */
class ColBankDepositInformationBuilder {
    private var bankAccountType: BankAccountType? = null
    private var bankAccountNumber: String? = null
    private var bankAccountHolderName: String? = null
    private var bankAccountHolderIdentificationType: IdentificationType? = null
    private var bankAccountHolderIdentificationValue: String? = null

    fun bankAccountType(type: BankAccountType) = apply { this.bankAccountType = type }
    fun bankAccountNumber(number: String) = apply { this.bankAccountNumber = number }
    fun bankAccountHolderName(name: String) = apply { this.bankAccountHolderName = name }
    fun bankAccountHolderIdentificationType(type: IdentificationType) = apply { this.bankAccountHolderIdentificationType = type }
    fun bankAccountHolderIdentificationValue(value: String) = apply { this.bankAccountHolderIdentificationValue = value }

    fun build(): ColBankDepositInformation {
        return ColBankDepositInformation(
            bankAccountType = requireNotNull(bankAccountType) { "bankAccountType is required" },
            bankAccountNumber = requireNotNull(bankAccountNumber) { "bankAccountNumber is required" },
            bankAccountHolderName = requireNotNull(bankAccountHolderName) { "bankAccountHolderName is required" },
            bankAccountHolderIdentificationType = requireNotNull(bankAccountHolderIdentificationType) { "bankAccountHolderIdentificationType is required" },
            bankAccountHolderIdentificationValue = requireNotNull(bankAccountHolderIdentificationValue) { "bankAccountHolderIdentificationValue is required" }
        )
    }
}

/**
 * Parameters for creating a Colombian bank withdrawal order
 */
data class CreateColBankOrderParams @JvmOverloads constructor(
    /** Rate signature from findRates */
    val rateSig: String,
    /** Source medium (e.g., "kusama", "card", "virtual") */
    val fromMedium: String,
    /** Destination medium (e.g., "bancolombia", "davivienda") - must match rate's toMediums */
    val toMedium: String,
    /** Bank deposit information */
    val depositInformation: ColBankDepositInformation,
    /** Amount in source currency (required if type is SRC) */
    val amountSrc: String? = null,
    /** Amount in destination currency (required if type is DST) */
    val amountDst: String? = null,
    /** Order type: SRC (specify source amount) or DST (specify destination amount) */
    val type: OrderType? = null,
    /** Arguments for auto-execution (account URN) */
    val args: ColBankOrderArgs? = null,
    /** Node ID for execution graph */
    val nodeId: String? = null,
    /** Optional metadata */
    val metadata: Map<String, String>? = null,
    /** Idempotency key for the request */
    val idempotencyKey: String? = null,
    /** Webhook URL to receive order status update notifications */
    val webhookUrl: String? = null
) {
    companion object {
        @JvmStatic
        fun builder() = CreateColBankOrderParamsBuilder()
    }
}

/**
 * Builder for CreateColBankOrderParams (Java-friendly)
 */
class CreateColBankOrderParamsBuilder {
    private var rateSig: String? = null
    private var fromMedium: String? = null
    private var toMedium: String? = null
    private var depositInformation: ColBankDepositInformation? = null
    private var amountSrc: String? = null
    private var amountDst: String? = null
    private var type: OrderType? = null
    private var args: ColBankOrderArgs? = null
    private var nodeId: String? = null
    private var metadata: Map<String, String>? = null
    private var idempotencyKey: String? = null
    private var webhookUrl: String? = null

    fun rateSig(rateSig: String) = apply { this.rateSig = rateSig }
    fun fromMedium(medium: String) = apply { this.fromMedium = medium }
    fun toMedium(medium: String) = apply { this.toMedium = medium }
    fun depositInformation(info: ColBankDepositInformation) = apply { this.depositInformation = info }
    fun amountSrc(amount: String) = apply { this.amountSrc = amount }
    fun amountDst(amount: String) = apply { this.amountDst = amount }
    fun type(type: OrderType) = apply { this.type = type }
    fun args(args: ColBankOrderArgs) = apply { this.args = args }
    fun accountUrn(urn: String) = apply { this.args = ColBankOrderArgs(accountUrn = urn) }
    fun nodeId(nodeId: String) = apply { this.nodeId = nodeId }
    fun metadata(metadata: Map<String, String>) = apply { this.metadata = metadata }
    fun idempotencyKey(key: String) = apply { this.idempotencyKey = key }
    fun webhookUrl(url: String) = apply { this.webhookUrl = url }

    fun build(): CreateColBankOrderParams {
        return CreateColBankOrderParams(
            rateSig = requireNotNull(rateSig) { "rateSig is required" },
            fromMedium = requireNotNull(fromMedium) { "fromMedium is required" },
            toMedium = requireNotNull(toMedium) { "toMedium is required" },
            depositInformation = requireNotNull(depositInformation) { "depositInformation is required" },
            amountSrc = amountSrc,
            amountDst = amountDst,
            type = type,
            args = args,
            nodeId = nodeId,
            metadata = metadata,
            idempotencyKey = idempotencyKey,
            webhookUrl = webhookUrl
        )
    }
}

/**
 * Result of creating a Colombian bank withdrawal order
 */
data class CreateColBankOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// US Bank ACH-Pull Order (external-us-bank -> Kusama, USD -> DUSD) - Request Models
// ============================================

/**
 * Execution arguments for a US-bank ACH-pull order.
 *
 * Resolved server-side into a Brale partner account/address mapping for the
 * linked bank; `expectedOwnerUrn` is asserted against the resolved owner so
 * a swap started by one user can never be silently re-pointed at a bank
 * linked by another.
 */
data class UsBankSwapArgs(
    /** Medium account URN on the `external-us-bank` medium (the linked bank to pull from). */
    val accountUrn: String,
    /** Swap order taker URN — typically the same value passed as `taker_urn`. */
    val expectedOwnerUrn: String
)

/**
 * Deposit information for a US-bank ACH-pull order: the Kreivo ledger
 * account that receives the minted DUSD.
 */
data class UsBankDepositInformation(
    val ledgerAccountId: String
)

data class CreateUsBankSwapOrderParams @JvmOverloads constructor(
    val rateSig: String,
    val depositInformation: UsBankDepositInformation,
    val args: UsBankSwapArgs,
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null,
    val webhookUrl: String? = null
)

data class CreateUsBankSwapOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
)

// ============================================
// US Bank ACH-Pull Order - Wire Models (Internal)
// ============================================

@Serializable
internal data class UsBankSwapArgsWire(
    @SerialName("account_urn") val accountUrn: String,
    @SerialName("expected_owner_urn") val expectedOwnerUrn: String
)

@Serializable
internal data class UsBankDepositInformationWire(
    @SerialName("ledger_account_id") val ledgerAccountId: String
)

@Serializable
internal data class CreateUsBankSwapOrderInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String = "external-us-bank",
    @SerialName("to_medium") val toMedium: String = "kusama",
    @SerialName("deposit_information") val depositInformation: UsBankDepositInformationWire,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    val args: UsBankSwapArgsWire,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)

// ============================================
// ColBank - Wire Models (Internal)
// ============================================

@Serializable
internal data class ColBankOrderArgsWire(
    @SerialName("account_urn") val accountUrn: String
)

@Serializable
internal data class ColBankDepositInformationWire(
    @SerialName("bank_account_type") val bankAccountType: String,
    @SerialName("bank_account_number") val bankAccountNumber: String,
    @SerialName("bank_account_holder_name") val bankAccountHolderName: String,
    @SerialName("bank_account_holder_identification_type") val bankAccountHolderIdentificationType: String,
    @SerialName("bank_account_holder_identification_value") val bankAccountHolderIdentificationValue: String
)

@Serializable
internal data class CreateColBankOrderInputWire(
    @SerialName("taker_urn") val takerUrn: String,
    val type: String,
    @SerialName("rate_sig") val rateSig: String,
    @SerialName("from_medium") val fromMedium: String,
    @SerialName("to_medium") val toMedium: String,
    @SerialName("amount_src") val amountSrc: String? = null,
    @SerialName("amount_dst") val amountDst: String? = null,
    val args: ColBankOrderArgsWire? = null,
    @SerialName("deposit_information") val depositInformation: ColBankDepositInformationWire,
    @SerialName("node_id") val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("webhook_url") val webhookUrl: String? = null
)
