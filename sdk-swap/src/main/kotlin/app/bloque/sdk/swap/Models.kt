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
