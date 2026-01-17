package app.bloque.sdk.swap

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for PSE (Pagos Seguros en Línea) bank utilities
 */
class PseClient internal constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Get list of available PSE banks
     *
     * @return ListBanksResult containing list of available banks
     */
    fun banks(): ListBanksResult {
        val response = httpClient.get<ListPseBanksResponseWire>(
            path = "/api/utils/pse/banks"
        )

        return ListBanksResult(
            banks = response.banks.map { mapBankResponse(it) }
        )
    }

    private fun mapBankResponse(wire: PseBankWire): Bank {
        return Bank(
            code = wire.financialInstitutionCode,
            name = wire.financialInstitutionName
        )
    }
}
