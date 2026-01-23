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

data class DepositInformation(
    val urn: String
)

data class CustomerData(
    val fullName: String
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
    val metadata: Map<String, String>? = null
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
    val name: String,
    val description: String,
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
    @SerialName("full_name") val fullName: String
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
    val name: String,
    val description: String,
    val how: ExecutionHowWire? = null,
    @SerialName("callback_token") val callbackToken: String? = null
)

@Serializable
internal data class ExecutionHowWire(
    val url: String? = null
)
