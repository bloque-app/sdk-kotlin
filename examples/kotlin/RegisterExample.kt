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
        origin = "bloque-root",
        apiKey = "sk_live_your_api_key_here",
        mode = Mode.PRODUCTION
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
    println("User URN: ${session.userUrn}")

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
    // Example 4: Register a business user
    // ============================================
    println("\n=== Example 4: Register Business User ===")

    val businessProfile = BusinessProfile(
        businessName = "Acme Corporation",
        industry = "Technology",
        email = "contact@acme.com",
        phone = "+1987654321",
        countryOfIncorporation = "USA",
        incorporationDate = "2015-06-15",
        addressLine1 = "456 Business Ave",
        addressLine2 = "Suite 100",
        city = "San Francisco",
        state = "CA",
        postalCode = "94102",
        country = "USA",
        documentType = "EIN",
        documentNumber = "12-3456789"
    )

    val businessSession = bloque.register(
        alias = "acme-corp",
        params = BusinessRegisterParams(businessProfile)
    )

    println("Business registered successfully!")
    println("Business URN: ${businessSession.userUrn}")

    // ============================================
    // Example 5: Register with minimal info (using defaults)
    // ============================================
    println("\n=== Example 5: Minimal Registration ===")

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
    println("User URN: ${minimalSession.userUrn}")
}
