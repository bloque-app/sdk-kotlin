package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.*
import app.bloque.sdk.compliance.GetKycVerificationParams
import app.bloque.sdk.compliance.KycVerificationParams
import app.bloque.sdk.core.*
import app.bloque.sdk.identity.BusinessProfile
import app.bloque.sdk.identity.BusinessRegisterParams
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile
import app.bloque.sdk.orgs.CreateOrgParams
import app.bloque.sdk.orgs.OrgProfile

/**
 * Kotlin usage examples for Bloque SDK
 *
 * Demonstrates idiomatic Kotlin usage with named parameters and DSL-like syntax
 */
fun main() {
    // Example 1: Creating the SDK (idiomatic Kotlin)
    val bloque = BloqueSDK.create(
        origin = "my-app-origin",
        apiKey = "sk_test_your_api_key_here",
        mode = Mode.SANDBOX
    )

    // Example 2: Connect to existing user (using alias: username, email, or phone)
    val session = bloque.connect("nestor")

    // Example 3: Create a Bancolombia account (using default params)
    val bancolombiaAccount = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(
            name = "Cuenta de Ahorros",
            metadata = mapOf(
                "purpose" to "savings",
                "category" to "personal"
            )
        )
    )

    println("Bancolombia Account URN: ${bancolombiaAccount.urn}")
    println("Reference Code: ${bancolombiaAccount.referenceCode}")

    // Example 4: Create a Card account
    val cardAccount = session.accounts.card.create(
        CreateCardAccountParams(
            name = "My Credit Card"
        )
    )

    println("Card Account URN: ${cardAccount.urn}")

    // Example 5: Transfer between accounts
    val transfer = session.accounts.transfer(
        TransferParams(
            sourceUrn = bancolombiaAccount.urn,
            destinationUrn = cardAccount.urn,
            amount = "100.00",
            asset = SupportedAsset.DUSD_6,
            metadata = mapOf("reference" to "monthly-transfer")
        )
    )

    println("Transfer Queue ID: ${transfer.queueId}")
    println("Transfer Status: ${transfer.status}")

    // Example 6: List card accounts with filters
    val cards = session.accounts.card.list(
        ListCardParams(status = "active")
    )

    println("Found ${cards.size} active card accounts")

    // Example 7: Get account balance
    val balances = session.accounts.card.balance(
        GetBalanceParams(
            urn = cardAccount.urn,
            asset = SupportedAsset.DUSD_6
        )
    )

    balances.forEach { (asset, balance) ->
        println("Asset: $asset")
        println("  Current: ${balance.current}")
        println("  Pending: ${balance.pending}")
        println("  In: ${balance.`in`}")
        println("  Out: ${balance.out}")
    }

    // Example 8: Register new individual user
    val registerParams = IndividualRegisterParams(
        profile = UserProfile(
            firstName = "John",
            lastName = "Doe",
            birthdate = "1990-01-01",
            email = "john@example.com",
            phone = "+1234567890",
            nationality = "US",
            countryOfResidence = "US",
            addressLine1 = "123 Main St",
            city = "New York",
            state = "NY",
            postalCode = "10001",
            country = "US",
            documentType = "passport",
            documentNumber = "AB123456"
        ),
        alias = "+1234567890",
        origin = "my-app-origin"
    )

    // Note: This would be called on a fresh SDK instance
    // val newUserSession = bloque.register("+1234567890", registerParams)

    // Example 9: Start KYC verification
    val kycResponse = session.compliance.kyc.startVerification(
        KycVerificationParams(
            urn = session.getUrn() ?: "",
            webhookUrl = "https://myapp.com/webhooks/kyc"
        )
    )

    println("KYC Verification URL: ${kycResponse.url}")
    println("KYC Status: ${kycResponse.status}")

    // Example 10: Create an organization
    val org = session.orgs.create(
        CreateOrgParams(
            profile = OrgProfile(
                legalName = "Acme Corporation",
                taxId = "123456789",
                incorporationDate = "2020-01-01",
                businessType = "LLC",
                incorporationCountryCode = "US",
                addressLine1 = "456 Business Ave",
                city = "San Francisco",
                state = "CA",
                postalCode = "94102",
                country = "US",
                website = "https://acme.com",
                email = "contact@acme.com",
                industry = "Technology"
            )
        )
    )

    println("Organization URN: ${org.urn}")
    println("Organization Status: ${org.status}")

    // Example 11: Update account status (chaining operations)
    val updatedAccount = session.accounts.bancolombia
        .freeze(bancolombiaAccount.urn)
        .also { println("Account frozen: ${it.status}") }
        .let { session.accounts.bancolombia.activate(it.urn) }
        .also { println("Account reactivated: ${it.status}") }

    // Example 12: Working with Polygon wallets
    val polygonAccount = session.accounts.polygon.create(
        CreatePolygonAccountParams(name = "My Polygon Wallet")
    )

    println("Polygon Wallet Address: ${polygonAccount.walletAddress}")

    // Example 13: Working with Virtual accounts
    val virtualAccount = session.accounts.virtual.create(
        CreateVirtualAccountParams(
            name = "Virtual Account",
            metadata = mapOf("type" to "escrow")
        )
    )

    println("Virtual Account Number: ${virtualAccount.accountNumber}")

    // Example 14: Error handling (Kotlin-style)
    runCatching {
        session.accounts.bancolombia.activate("invalid-urn")
    }.onFailure { exception ->
        when (exception) {
            is BloqueNotFoundError -> println("Account not found: ${exception.message}")
            is BloqueRateLimitError -> println("Rate limit exceeded: ${exception.message}")
            is BloqueAuthenticationError -> println("Authentication failed: ${exception.message}")
            is BloqueAPIError -> println("API Error (${exception.statusCode}): ${exception.message}")
            is BloqueException -> println("SDK Error: ${exception.message}")
            else -> println("Unexpected error: ${exception.message}")
        }
    }

    // Example 15: Update metadata
    val updatedBancolombia = session.accounts.bancolombia.updateMetadata(
        UpdateBancolombiaMetadataParams(
            urn = bancolombiaAccount.urn,
            metadata = mapOf(
                "name" to "Updated Savings Account",
                "category" to "business"
            )
        )
    )

    println("Updated account name in metadata: ${updatedBancolombia.metadata["name"]}")

    // Example 16: Get account movements
    val movements = session.accounts.card.movements(
        ListMovementsParams(
            urn = cardAccount.urn,
            limit = 10,
            offset = 0
        )
    )

    movements.forEach { movement ->
        println("Movement: ${movement.amount} ${movement.asset} - ${movement.direction}")
    }

    // Example 17: Business registration
    val businessRegisterParams = BusinessRegisterParams(
        profile = BusinessProfile(
            legalName = "Tech Startup Inc",
            taxId = "987654321",
            incorporationDate = "2023-01-01",
            businessType = "Corporation",
            incorporationCountryCode = "US",
            addressLine1 = "789 Startup Blvd",
            city = "Austin",
            postalCode = "78701",
            country = "US",
            email = "hello@techstartup.com"
        ),
        alias = "techstartup@email.com",
        origin = "my-app-origin"
    )

    // val businessSession = bloque.register("techstartup@email.com", businessRegisterParams)

    println("\nAll examples completed successfully!")
}
