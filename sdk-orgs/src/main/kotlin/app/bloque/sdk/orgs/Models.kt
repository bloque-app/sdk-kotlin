package app.bloque.sdk.orgs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================
// Organization Types
// ============================================

/**
 * Organization type
 */
enum class OrgType {
    @SerialName("business")
    BUSINESS,

    @SerialName("individual")
    INDIVIDUAL
}

/**
 * Organization status
 */
enum class OrgStatus {
    @SerialName("awaiting_compliance_verification")
    AWAITING_COMPLIANCE_VERIFICATION,

    @SerialName("active")
    ACTIVE,

    @SerialName("suspended")
    SUSPENDED,

    @SerialName("closed")
    CLOSED
}

/**
 * Organization profile
 */
data class OrgProfile @JvmOverloads constructor(
    val legalName: String,
    val taxId: String,
    val incorporationDate: String,
    val businessType: String,
    val incorporationCountryCode: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    val postalCode: String,
    val country: String,
    val website: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val industry: String? = null
)

/**
 * Organization entity
 */
data class Organization @JvmOverloads constructor(
    val urn: String,
    val orgType: OrgType,
    val profile: OrgProfile,
    val metadata: Map<String, String?> = emptyMap(),
    val status: OrgStatus
)

/**
 * Parameters for creating an organization
 */
data class CreateOrgParams @JvmOverloads constructor(
    val profile: OrgProfile,
    val metadata: Map<String, String?> = emptyMap()
)

// ============================================
// Wire Types (Internal)
// ============================================

@Serializable
internal data class OrgProfileWire(
    @SerialName("legal_name") val legalName: String,
    @SerialName("tax_id") val taxId: String,
    @SerialName("incorporation_date") val incorporationDate: String,
    @SerialName("business_type") val businessType: String,
    @SerialName("incorporation_country_code") val incorporationCountryCode: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String,
    val country: String,
    val website: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val industry: String? = null
)

@Serializable
internal data class OrganizationWire(
    val urn: String,
    @SerialName("org_type") val orgType: String,
    val profile: OrgProfileWire,
    val metadata: Map<String, String?> = emptyMap(),
    val status: String
)

@Serializable
internal data class CreateOrgRequestWire(
    @SerialName("org_type") val orgType: String = "business",
    val profile: OrgProfileWire,
    val metadata: Map<String, String?> = emptyMap()
)

@Serializable
internal data class CreateOrgResponseWire(
    val result: CreateOrgResultWire
)

@Serializable
internal data class CreateOrgResultWire(
    val organization: OrganizationWire
)
