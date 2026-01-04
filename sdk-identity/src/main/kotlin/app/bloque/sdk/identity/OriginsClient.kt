package app.bloque.sdk.identity

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

/**
 * Client for origin operations
 */
class OriginsClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * WhatsApp origin client
     */
    val whatsapp: OriginClient<OTPAssertionWhatsApp> = OriginClient(httpClient, "bloque-whatsapp")

    /**
     * Email origin client
     */
    val email: OriginClient<OTPAssertionEmail> = OriginClient(httpClient, "bloque-email")

    /**
     * Create a custom origin client
     *
     * @param origin The origin name
     * @return Origin client for the specified origin
     */
    fun custom(origin: String): OriginClient<OTPAssertion> {
        return OriginClient(httpClient, origin)
    }

    /**
     * List all available origins
     *
     * @return List of available origins
     */
    fun list(): List<Origin> {
        val response = httpClient.get<OriginListResponse>(
            path = "/api/origins"
        )

        return response.result.origins
    }

    /**
     * Register a new identity
     *
     * @param params Registration parameters (individual or business)
     * @return Registration result with URN and access token
     */
    fun register(params: RegisterParams): RegisterResult {
        val profileMap = when (params) {
            is IndividualRegisterParams -> {
                val profile = params.profile
                buildMap<String, String?> {
                    put("org_type", "individual")
                    profile.firstName?.let { put("first_name", it) }
                    profile.lastName?.let { put("last_name", it) }
                    profile.birthdate?.let { put("birthdate", it) }
                    profile.email?.let { put("email", it) }
                    profile.phone?.let { put("phone", it) }
                    profile.nationality?.let { put("nationality", it) }
                    profile.countryOfResidence?.let { put("country_of_residence", it) }
                    profile.addressLine1?.let { put("address_line1", it) }
                    profile.addressLine2?.let { put("address_line2", it) }
                    profile.city?.let { put("city", it) }
                    profile.state?.let { put("state", it) }
                    profile.postalCode?.let { put("postal_code", it) }
                    profile.country?.let { put("country", it) }
                    profile.documentType?.let { put("document_type", it) }
                    profile.documentNumber?.let { put("document_number", it) }
                    profile.documentIssueDate?.let { put("document_issue_date", it) }
                    profile.documentExpiryDate?.let { put("document_expiry_date", it) }
                }
            }

            is BusinessRegisterParams -> {
                val profile = params.profile
                buildMap<String, String?> {
                    put("org_type", "business")
                    put("legal_name", profile.legalName)
                    put("tax_id", profile.taxId)
                    put("incorporation_date", profile.incorporationDate)
                    put("business_type", profile.businessType)
                    put("incorporation_country_code", profile.incorporationCountryCode)
                    put("address_line1", profile.addressLine1)
                    profile.addressLine2?.let { put("address_line2", it) }
                    put("city", profile.city)
                    profile.state?.let { put("state", it) }
                    put("postal_code", profile.postalCode)
                    put("country", profile.country)
                    profile.website?.let { put("website", it) }
                    profile.email?.let { put("email", it) }
                    profile.phone?.let { put("phone", it) }
                    profile.industry?.let { put("industry", it) }
                }
            }
        }

        val orgType = when (params) {
            is IndividualRegisterParams -> "individual"
            is BusinessRegisterParams -> "business"
        }

        val request = RegisterRequestWire(
            profile = profileMap,
            orgType = orgType,
            alias = params.alias,
            origin = params.origin,
            metadata = params.metadata
        )

        val response = httpClient.post<RegisterResponseWire, RegisterRequestWire>(
            path = "/api/origins/${params.origin}/register",
            body = request
        )

        return RegisterResult(
            urn = response.result.urn,
            accessToken = response.result.accessToken
        )
    }
}
