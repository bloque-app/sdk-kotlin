package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueConfigError
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
     * Colombian bank withdrawal client (Bancolombia)
     */
    val colbank: ColBankClient = ColBankClient(httpClient)

    /**
     * BRE-B withdrawal client
     */
    val breb: BrebClient = BrebClient(httpClient)

    /**
     * List orders for the current user (as taker)
     *
     * Retrieves all swap orders taken by the authenticated user.
     * Can optionally filter by status, maker, date range, etc.
     *
     * Usage (Kotlin):
     * ```kotlin
     * // List all orders
     * val allOrders = session.swap.listOrders()
     *
     * // List completed orders only
     * val completedOrders = session.swap.listOrders(
     *     ListOrdersParams(status = OrderStatus.COMPLETED)
     * )
     *
     * // List orders with multiple filters
     * val filteredOrders = session.swap.listOrders(
     *     ListOrdersParams(
     *         status = OrderStatus.PENDING,
     *         after = 1705315200000L
     *     )
     * )
     * ```
     *
     * @param params Optional parameters for filtering orders
     * @return ListOrdersResult containing list of orders
     */
    @JvmOverloads
    fun listOrders(params: ListOrdersParams = ListOrdersParams()): ListOrdersResult {
        val takerUrn = httpClient.getUrn()
            ?: throw BloqueConfigError("User URN is not available. Please connect to a session first.")

        val encodedUrn = URLEncoder.encode(takerUrn, "UTF-8")
        val queryParams = buildListOrdersQueryParams(params)
        val url = "/api/order/taker/$encodedUrn$queryParams"

        val response = httpClient.get<ListOrdersResponseWire>(path = url)

        return ListOrdersResult(
            orders = response.orders.map { mapOrderResponse(it) }
        )
    }

    private fun buildListOrdersQueryParams(params: ListOrdersParams): String {
        val queryParts = mutableListOf<String>()

        params.status?.let { queryParts.add("status=${it.value}") }
        params.makerUrn?.let { queryParts.add("maker_urn=${URLEncoder.encode(it, "UTF-8")}") }
        params.orderSig?.let { queryParts.add("order_sig=$it") }
        params.swapSig?.let { queryParts.add("swap_sig=$it") }
        params.rateSig?.let { queryParts.add("rate_sig=$it") }
        params.graphId?.let { queryParts.add("graph_id=$it") }
        params.after?.let { queryParts.add("after=$it") }
        params.before?.let { queryParts.add("before=$it") }

        return if (queryParts.isEmpty()) "" else "?" + queryParts.joinToString("&")
    }

    private fun mapOrderResponse(wire: OrderWire): SwapOrder {
        return SwapOrder(
            id = wire.id,
            orderSig = wire.orderSig,
            rateSig = wire.rateSig,
            swapSig = wire.swapSig,
            taker = wire.taker,
            maker = wire.maker,
            fromAsset = wire.fromAsset,
            toAsset = wire.toAsset,
            fromMedium = wire.fromMedium,
            toMedium = wire.toMedium,
            fromAmount = wire.fromAmount,
            toAmount = wire.toAmount,
            at = wire.at,
            graphId = wire.graphId,
            status = wire.status,
            metadata = wire.metadata,
            createdAt = wire.createdAt,
            updatedAt = wire.updatedAt
        )
    }

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
