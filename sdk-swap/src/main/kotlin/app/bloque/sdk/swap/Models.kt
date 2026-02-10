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
    val amount: Double? = null
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
    val amount: Double? = null
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
    val amountSrc: String? = null,
    val amountDst: String? = null,
    val type: OrderType? = null,
    val args: PseOrderArgs? = null,
    val nodeId: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null
)

// ============================================
// PSE Create Order - Response Models
// ============================================

data class CreatePseOrderResult(
    val order: SwapOrder,
    val execution: ExecutionResult?,
    val requestId: String
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
    val metadata: Map<String, String>? = null
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
    @SerialName("bank_code") val bankCode: String,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("user_legal_id_type") val userLegalIdType: String? = null,
    @SerialName("user_legal_id") val userLegalId: String? = null,
    @SerialName("customer_data") val customerData: CustomerDataWire? = null
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
    val idempotencyKey: String? = null
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
            idempotencyKey = idempotencyKey
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
    val metadata: Map<String, String>? = null
)
