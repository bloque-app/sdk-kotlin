# Bloque SDK for Kotlin/Java

Official Bloque SDK for Kotlin and Java applications.

## 📦 Build the Library

```bash
# Build
./gradlew build

# Publish to Maven Local (~/.m2/repository)
./gradlew publishToMavenLocal
```

## 🔧 Installation

### Gradle (Kotlin DSL) - **Recommended**

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    // You only need the main module
    implementation("app.bloque.sdk:sdk:0.0.9")
}
```

### Gradle (Groovy)

```groovy
// build.gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'app.bloque.sdk:sdk:0.0.9'
}
```

### Maven

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>app.bloque.sdk</groupId>
        <artifactId>sdk</artifactId>
        <version>0.0.9</version>
    </dependency>
</dependencies>
```

## 💻 Usage

### Important: Working with Aliases

- **Always use aliases** when calling `connect()` and `register()` (e.g., username `"nestor"`, email `"user@email.com"`, phone `"+1234567890"`)
- **URNs are handled automatically** - The SDK constructs and manages URNs internally
- **URNs in responses** - You'll receive URNs in API responses (e.g., `account.urn`), which you use to reference those resources in subsequent operations

### Kotlin

```kotlin
import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.accounts.CreateBancolombiaAccountParams

fun main() {
    val bloque = BloqueSDK.create(
        origin = "my-app-origin",  // Your app's origin
        apiKey = "sk_live_...",
        mode = Mode.PRODUCTION
    )

    // Connect using alias (username, phone number, email, etc.)
    val session = bloque.connect("nestor")

    // Create an account - URN is returned in the response
    val account = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(name = "Savings")
    )

    println("Account created: ${account.urn}")

    // Use the account URN for subsequent operations
    session.accounts.bancolombia.freeze(account.urn)
}
```

### Java

```java
import app.bloque.sdk.BloqueSDK;
import app.bloque.sdk.core.Mode;
import app.bloque.sdk.UserSession;
import app.bloque.sdk.accounts.BancolombiaAccount;
import app.bloque.sdk.accounts.CreateBancolombiaAccountParams;

public class Main {
    public static void main(String[] args) {
        BloqueSDK bloque = BloqueSDK.builder()
            .origin("my-app-origin")  // Your app's origin
            .apiKey("sk_live_...")
            .mode(Mode.PRODUCTION)
            .build();

        // Connect using alias (username, phone number, email, etc.)
        UserSession session = bloque.connect("nestor");

        // Create an account - URN is returned in the response
        BancolombiaAccount account = session.getAccounts()
            .getBancolombia()
            .create(new CreateBancolombiaAccountParams("Savings", null, null, null, null));

        System.out.println("Account created: " + account.getUrn());

        // Use the account URN for subsequent operations
        session.getAccounts().getBancolombia().freeze(account.getUrn());
    }
}
```

## 📚 Modules

The SDK is organized into modular packages:

| Artifact | Description |
|----------|-------------|
| `app.bloque.sdk:sdk` | **Main SDK** - Complete SDK (includes all modules below) |
| `app.bloque.sdk:sdk-core` | Core HTTP client, errors, and base configuration |
| `app.bloque.sdk:sdk-accounts` | Account operations (Bancolombia, Card, Virtual, Polygon) |
| `app.bloque.sdk:sdk-identity` | Identity and authentication (Aliases, Origins, Registration) |
| `app.bloque.sdk:sdk-compliance` | Compliance operations (KYC/KYB) |
| `app.bloque.sdk:sdk-orgs` | Organization management |

## 🎯 Features

### ✅ Complete TypeScript SDK Parity
This SDK is a complete port of the TypeScript SDK with 100% feature parity:

- **Accounts**: Create and manage Bancolombia, Card, Virtual, and Polygon accounts
- **Identity**: User registration, aliases, and multi-origin authentication
- **Compliance**: KYC/KYB verification workflows
- **Organizations**: Business account management
- **Transfers**: Move funds between accounts with multiple asset support

### ✅ Full Java Compatibility
- Builder pattern for Java-friendly object construction
- Proper `@JvmOverloads` annotations for default parameters
- Java-compatible getters (e.g., `getUrn()`, `getStatus()`)
- Exception hierarchy that works with Java's try-catch

### ✅ Kotlin Idiomatic Design
- Data classes for immutability
- Sealed classes for type-safe unions
- Named parameters and default values
- Null safety with Kotlin's type system
- Extension functions where appropriate

## 📖 Detailed Examples

Check out the comprehensive examples:
- [Kotlin Examples](examples/kotlin/KotlinExample.kt) - Idiomatic Kotlin usage with all features
- [Java Examples](examples/java/JavaExample.java) - Java-friendly patterns and usage
- [Shared Ledger Example (Kotlin)](examples/kotlin/SharedLedgerExample.kt) - Linking multiple accounts to the same ledger
- [Shared Ledger Example (Java)](examples/java/SharedLedgerExample.java) - Linking multiple accounts to the same ledger
- [Ledger Explanation](examples/LEDGER_EXPLANATION.md) - Understanding ledgers, accounts, and use cases

### Account Management

```kotlin
// Create different account types
val bancolombia = session.accounts.bancolombia.create(
    CreateBancolombiaAccountParams(name = "Savings")
)

val card = session.accounts.card.create(
    CreateCardAccountParams(name = "Credit Card")
)

val polygon = session.accounts.polygon.create(
    CreatePolygonAccountParams(name = "Crypto Wallet")
)

// Transfer between accounts
val transfer = session.accounts.transfer(
    TransferParams(
        sourceUrn = bancolombia.urn,
        destinationUrn = card.urn,
        amount = "100.00",
        asset = SupportedAsset.DUSD_6
    )
)
```

### Shared Ledger (Multiple Accounts, Same Balance)

```kotlin
// Create virtual account (creates a new ledger)
val virtualAccount = session.accounts.virtual.create(
    CreateVirtualAccountParams(name = "Main Account")
)

// Get the ledger ID
val sharedLedgerId = virtualAccount.ledgerId!!

// Create card linked to the SAME ledger
val cardAccount = session.accounts.card.create(
    CreateCardAccountParams(
        name = "My Card",
        ledgerId = sharedLedgerId  // Share balance with virtual account
    )
)

// Both accounts now share the same balance!
// Transfers between them are instant
```

### Identity & Registration

```kotlin
// Register new user
val userProfile = UserProfile(
    firstName = "John",
    lastName = "Doe",
    email = "john@example.com",
    // ... more fields
)

val session = bloque.register(
    alias = "+1234567890",
    params = IndividualRegisterParams(
        profile = userProfile
    )
)
```

### Compliance (KYC)

```kotlin
// Start KYC verification
val kycResponse = session.compliance.kyc.startVerification(
    KycVerificationParams(
        urn = session.getUrn()!!,
        webhookUrl = "https://myapp.com/webhook"
    )
)

println("Complete KYC at: ${kycResponse.url}")
```

### Organizations

```kotlin
// Create business organization
val org = session.orgs.create(
    CreateOrgParams(
        profile = OrgProfile(
            legalName = "Acme Corp",
            taxId = "123456789",
            // ... more fields
        )
    )
)
```

## 🔧 Error Handling

The SDK provides a comprehensive error hierarchy:

```kotlin
try {
    session.accounts.bancolombia.activate("invalid-urn")
} catch (e: BloqueNotFoundError) {
    // Handle 404 errors
} catch (e: BloqueRateLimitError) {
    // Handle 429 rate limit
} catch (e: BloqueAuthenticationError) {
    // Handle 401/403 auth errors
} catch (e: BloqueValidationError) {
    // Handle 400 validation errors
} catch (e: BloqueAPIError) {
    // Handle other API errors
    println("Error ${e.statusCode}: ${e.message}")
} catch (e: BloqueNetworkError) {
    // Handle network failures
} catch (e: BloqueException) {
    // Handle all other SDK errors
}
```

## 🏗️ Architecture

### Design Patterns
- **Builder Pattern**: Java-friendly SDK initialization
- **Facade Pattern**: Unified API through `BloqueSDK` and `UserSession`
- **Factory Pattern**: Error creation based on HTTP status codes
- **Template Method**: `BaseClient` for all API clients

### Best Practices
- **Modular Design**: Each feature in its own Gradle module
- **Separation of Concerns**: Public DTOs vs internal wire types
- **Type Safety**: Sealed classes for unions, enums for constants
- **Immutability**: Data classes are immutable by default
- **Java Interop**: `@JvmOverloads`, `@JvmStatic`, proper visibility

## 🛠️ Requirements

- **Java**: 17 or higher
- **Kotlin**: 2.0+ (for Kotlin projects)
- **Gradle**: 8.0+ (included via wrapper)

## 🚀 Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/bloque-sdk-kotlin.git
cd bloque-sdk-kotlin

# Build all modules
./gradlew build

# Run tests
./gradlew test

# Publish to Maven Local
./gradlew publishToMavenLocal

# Generate documentation
./gradlew dokkaHtml
```

## 📝 License

[MIT](./LICENSE)

---

**Note**: This SDK is a complete port of the [Bloque TypeScript SDK](https://github.com/bloque/sdk) with full feature parity and Java compatibility.
