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
    mavenLocal()  // For local development
    mavenCentral()
}

dependencies {
    // You only need the main module
    implementation("app.bloque.sdk:sdk:0.1.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
// build.gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'app.bloque.sdk:sdk:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<!-- pom.xml -->
<repositories>
    <repository>
        <id>local</id>
        <url>file://${user.home}/.m2/repository</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>app.bloque.sdk</groupId>
        <artifactId>sdk</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## 💻 Usage

### Kotlin

```kotlin
import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.accounts.CreateBancolombiaAccountParams

fun main() {
    val bloque = BloqueSDK.create(
        origin = "bloque-root",
        apiKey = "sk_live_...",
        mode = Mode.PRODUCTION
    )

    val session = bloque.connect("did:bloque:bloque-whatsapp:573023348486")

    val account = session.accounts.bancolombia.create(
        CreateBancolombiaAccountParams(name = "Savings")
    )

    println("Account created: ${account.urn}")
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
            .origin("bloque-root")
            .apiKey("sk_live_...")
            .mode(Mode.PRODUCTION)
            .build();

        UserSession session = bloque.connect("did:bloque:bloque-whatsapp:573023348486");

        BancolombiaAccount account = session.getAccounts()
            .getBancolombia()
            .create(new CreateBancolombiaAccountParams("Savings", null, null, null));

        System.out.println("Account created: " + account.getUrn());
    }
}
```

## 📚 Modules

| Artifact | Description |
|----------|-------------|
| `app.bloque.sdk:sdk` | Complete SDK (includes all modules) |
| `app.bloque.sdk:sdk-core` | HTTP client and base configuration |
| `app.bloque.sdk:sdk-accounts` | Accounts module only |

## 🛠️ Requirements

- Java 17+
- Kotlin 2.0+ (for Kotlin projects)

## License

[MIT](./LICENSE)
