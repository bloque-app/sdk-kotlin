package app.bloque.sdk.accounts

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Main client for all account operations
 */
class AccountsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Bancolombia account operations
     */
    val bancolombia: BancolombiaClient = BancolombiaClient(httpClient)

    /**
     * Card account operations
     */
    val card: CardClient = CardClient(httpClient)

    /**
     * Virtual account operations
     */
    val virtual: VirtualClient = VirtualClient(httpClient)

    /**
     * Polygon wallet account operations
     */
    val polygon: PolygonClient = PolygonClient(httpClient)

    /**
     * Transfer funds between accounts
     *
     * @param params Transfer parameters (source, destination, amount, asset)
     * @return Transfer result with queue ID and status
     */
    fun transfer(params: TransferParams): TransferResult {
        val request = TransferRequest(
            sourceUrn = params.sourceUrn,
            destinationUrn = params.destinationUrn,
            amount = params.amount,
            asset = params.asset.value,
            metadata = params.metadata.mapValues { it.value.toString() }
        )

        val response = httpClient.post<TransferResponseWrapper, TransferRequest>(
            path = "/api/transfers",
            body = request
        )

        return TransferResult(
            queueId = response.result.queueId,
            status = response.result.status,
            message = response.result.message
        )
    }
}
