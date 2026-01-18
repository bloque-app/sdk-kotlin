package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.*
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.*

/**
 * Kotlin example: Register a new user
 *
 * This example demonstrates how to register individual and business users
 * using the Bloque SDK in idiomatic Kotlin.
 */
fun main() {
    // Initialize the SDK
    val bloque = BloqueSDK.create(
        origin = "origin",
        apiKey = "api-key",
        mode = Mode.SANDBOX
    )

    // ============================================
    // Example 1: Register an individual user
    // ============================================
    println("=== Example 1: Register Individual User ===")

    val profile = UserProfile(
        firstName = "John",
        lastName = "Doe",
        birthdate = "1990-01-01",
        email = "john.doe@example.com",
        phone = "+1234567890",
        nationality = "USA",
        countryOfResidence = "USA",
        addressLine1 = "123 Main Street",
        addressLine2 = "Apt 4B",
        city = "New York",
        state = "NY",
        postalCode = "10001",
        country = "USA",
        documentType = "SSN",
        documentNumber = "123-45-6789"
    )

    val session = bloque.register(
        alias = "johndoe123",
        params = IndividualRegisterParams(profile)
    )

    println("User registered successfully!")
    println("User URN: ${session.getUrn()}")

    // ============================================
    // Example 2: List user's virtual accounts
    // ============================================
    println("\n=== Example 2: List Virtual Accounts ===")

    val virtualAccounts = session.accounts.virtual.list()

    println("Total virtual accounts: ${virtualAccounts.size}")

    virtualAccounts.forEach { account ->
        println("  - URN: ${account.urn}")
        println("    Status: ${account.status}")
    }

    // ============================================
    // Example 3: Create accounts for the new user
    // ============================================
    println("\n=== Example 3: Create Accounts ===")

    // Create a card account
    val cardAccount = session.accounts.card.create(
        CreateCardAccountParams(name = "My First Card")
    )

    println("Card Account created: ${cardAccount.urn}")

    // Create a virtual account
    val virtualAccount = session.accounts.virtual.create(
        CreateVirtualAccountParams(name = "My Savings")
    )

    println("Virtual Account created: ${virtualAccount.urn}")

    // ============================================
    // Example 4: Register a basic business (minimal fields)
    // ============================================
    println("\n=== Example 4: Register Basic Business ===")

    // BasicBusinessProfile requires only the essential fields:
    // - name: display name
    // - legalName: official registered name
    // - taxId: NIT, EIN, or tax identifier
    // - businessType: SAS, SA, LTDA, SL, LLC, CORP, or OTHER
    // - email: contact email
    // - incorporationDate: date of incorporation (YYYY-MM-DD)
    // - country: country code (e.g., "CO", "US")
    // - phone: optional contact phone
    val basicBusinessProfile = BasicBusinessProfile(
        name = "Acme Corp",
        legalName = "Acme Corporation S.A.S.",
        taxId = "900123456-1",
        businessType = BusinessType.SAS,
        email = "contact@acme.com",
        incorporationDate = "2020-03-15",
        country = "COL",
        phone = "+573001234567"
    )

    val basicBusinessSession = bloque.register(
        alias = "acme-corp",
        params = BasicBusinessRegisterParams(basicBusinessProfile)
    )

    println("Basic Business registered successfully!")
    println("Business URN: ${basicBusinessSession.getUrn()}")

    // ============================================
    // Example 5: Register a full business (with address)
    // ============================================
    println("\n=== Example 5: Register Full Business ===")

    // BusinessProfile includes all fields for full KYB
    val fullBusinessProfile = BusinessProfile(
        name = "TechStart Solutions",
        legalName = "TechStart Solutions S.A.S.",
        taxId = "901987654-3",
        incorporationDate = "2018-06-20",
        businessType = "SAS",
        incorporationCountryCode = "CO",
        addressLine1 = "Calle 100 #15-20",
        addressLine2 = "Oficina 1501",
        city = "Bogotá",
        state = "Cundinamarca",
        postalCode = "110111",
        country = "COL",
        website = "https://techstart.co",
        email = "info@techstart.co",
        phone = "+573109876543",
        industry = "Technology"
    )

    val fullBusinessSession = bloque.register(
        alias = "techstart",
        params = BusinessRegisterParams(fullBusinessProfile)
    )

    println("Full Business registered successfully!")
    println("Business URN: ${fullBusinessSession.getUrn()}")

    // ============================================
    // Example 6: Register with minimal individual info
    // ============================================
    println("\n=== Example 6: Minimal Individual Registration ===")

    val minimalProfile = UserProfile(
        firstName = "Jane",
        lastName = "Smith",
        birthdate = "1985-05-20",
        email = "jane.smith@example.com",
        phone = "+1555123456",
        nationality = "USA",
        countryOfResidence = "USA",
        addressLine1 = "789 Simple St",
        city = "Boston",
        state = "MA",
        postalCode = "02101",
        country = "USA",
        documentType = "PASSPORT",
        documentNumber = "AB1234567"
    )

    val minimalSession = bloque.register(
        alias = "janesmith",
        params = IndividualRegisterParams(minimalProfile)
    )

    println("Minimal registration completed!")
    println("User URN: ${minimalSession.getUrn()}")

    // ============================================
    // Example 7: Basic individual (phone only)
    // ============================================
    println("\n=== Example 7: Basic Individual (Phone Only) ===")

    // BasicUserProfile requires only a phone number
    val basicUserProfile = BasicUserProfile(
        phoneNumber = "+573001234567"
    )

    val basicUserSession = bloque.register(
        alias = "phone-user",
        params = BasicIndividualRegisterParams(basicUserProfile)
    )

    println("Basic user registered with phone!")
    println("User URN: ${basicUserSession.getUrn()}")

    println("\n✅ All examples completed successfully!")
}
