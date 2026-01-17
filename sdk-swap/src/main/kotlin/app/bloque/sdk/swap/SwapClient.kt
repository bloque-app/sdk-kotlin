package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Client for swap operations - finding exchange rates between assets
 */
class SwapClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    private val json = Json { encodeDefaults = true }

    /**
     * PSE (bank) utilities client
     */
    val pse: PseClient = PseClient(httpClient)

    /**
     * Find available exchange rates between assets
     *
     * @param params Parameters for finding rates including assets, mediums, and amounts
     * @return FindRatesResult containing list of available swap rates
     */
    fun findRates(params: FindRatesParams): FindRatesResult {
        val queryParams = buildQueryParams(params)
        val url = "/api/rates$queryParams"

        val response = httpClient.get<FindRatesResponseWire>(
            path = url
        )

        return FindRatesResult(
            rates = response.rates.map { mapRateResponse(it) }
        )
    }

    private fun buildQueryParams(params: FindRatesParams): String {
        val queryParts = mutableListOf<String>()

        // Edge parameter: [fromAsset, toAsset]
        val edge = json.encodeToString(
            ListSerializer(String.serializer()),
            listOf(params.fromAsset, params.toAsset)
        )
        queryParts.add("edge=${URLEncoder.encode(edge, "UTF-8")}")

        // From mediums
        val fromMedium = json.encodeToString(
            ListSerializer(String.serializer()),
            params.fromMediums
        )
        queryParts.add("from_medium=${URLEncoder.encode(fromMedium, "UTF-8")}")

        // To mediums
        val toMedium = json.encodeToString(
            ListSerializer(String.serializer()),
            params.toMediums
        )
        queryParts.add("to_medium=${URLEncoder.encode(toMedium, "UTF-8")}")

        // Optional parameters
        params.amountSrc?.let { queryParts.add("amount_src=$it") }
        params.amountDst?.let { queryParts.add("amount_dst=$it") }
        params.sort?.let { queryParts.add("sort=${it.value}") }
        params.sortBy?.let { queryParts.add("sort_by=${it.value}") }

        return "?" + queryParts.joinToString("&")
    }

    private fun mapRateResponse(wire: RateWire): SwapRate {
        return SwapRate(
            id = wire.id,
            sig = wire.sig,
            swapSig = wire.swapSig,
            maker = wire.maker,
            edge = Pair(wire.edge[0], wire.edge[1]),
            fee = mapFeeResponse(wire.fee),
            at = wire.at,
            until = wire.until,
            fromMediums = wire.fromMedium,
            toMediums = wire.toMedium,
            rate = Pair(wire.rate[0], wire.rate[1]),
            ratio = wire.ratio,
            fromLimits = Pair(wire.fromLimits[0], wire.fromLimits[1]),
            toLimits = Pair(wire.toLimits[0], wire.toLimits[1]),
            createdAt = wire.createdAt,
            updatedAt = wire.updatedAt
        )
    }

    private fun mapFeeResponse(wire: FeeWire): Fee {
        return Fee(
            at = wire.at,
            value = wire.value,
            formula = wire.formula,
            components = wire.components.map { mapFeeComponentResponse(it) }
        )
    }

    private fun mapFeeComponentResponse(wire: FeeComponentWire): FeeComponent {
        return FeeComponent(
            at = wire.at,
            name = wire.name,
            type = FeeComponentType.fromString(wire.type),
            value = wire.value,
            percentage = wire.percentage,
            pair = wire.pair,
            amount = wire.amount
        )
    }
}
