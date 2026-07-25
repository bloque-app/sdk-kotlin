package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.accounts.ActivateBrebKeyParams
import app.bloque.sdk.accounts.BrebKeyType
import app.bloque.sdk.accounts.CreateBrebKeyParams
import app.bloque.sdk.accounts.DeleteBrebKeyParams
import app.bloque.sdk.accounts.ResolveBrebKeyParams
import app.bloque.sdk.accounts.SuspendBrebKeyParams
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.IndividualRegisterParams
import app.bloque.sdk.identity.UserProfile

/**
 * Kotlin example: BRE-B key operations
 *
 * This example demonstrates how to create, resolve, suspend, activate,
 * and delete BRE-B keys using the Bloque SDK in idiomatic Kotlin.
 */
fun main() {
    val bloque = BloqueSDK.create(
        origin = "{your-origin-here}",
        apiKey = "{your-api-key-here}",
        mode = Mode.SANDBOX
    )

    val session = bloque.register(
        "example-user-breb",
        IndividualRegisterParams(
            UserProfile(
                firstName = "Example",
                lastName = "User",
                email = "example-breb@example.com",
                phone = "+573123185778"
            )
        )
    )

    println("=== Example 1: Create BRE-B Key ===")

    val created = session.accounts.breb.createKey(
        CreateBrebKeyParams(
            keyType = BrebKeyType.PHONE,
            key = "3123185778",
            displayName = "Pepito Silva",
            ledgerId = "ledger-account-breb-001",
            metadata = mapOf(
                "source" to "example",
                "channel" to "sdk-kotlin"
            )
        )
    )

    println("Create error: ${created.error?.message}")
    println("Account URN: ${created.data?.urn}")
    println("Remote Key ID: ${created.data?.remoteKeyId}")
    println("Status: ${created.data?.status}")

    println("\n=== Example 2: Resolve BRE-B Key ===")

    val resolved = session.accounts.breb.resolveKey(
        ResolveBrebKeyParams(
            keyType = BrebKeyType.PHONE,
            key = "3123185778"
        )
    )

    println("Resolve error: ${resolved.error?.message}")
    println("Resolution ID: ${resolved.data?.resolutionId}")
    println("Owner name: ${resolved.data?.owner?.name}")
    println("Receptor node: ${resolved.data?.receptorNode}")

    created.data?.let { account ->
        println("\n=== Example 3: Suspend BRE-B Key ===")

        val suspended = session.accounts.breb.suspendKey(
            SuspendBrebKeyParams(accountUrn = account.urn)
        )

        println("Suspend error: ${suspended.error?.message}")
        println("Suspend status: ${suspended.data?.status}")
        println("Upstream key status: ${suspended.data?.keyStatus}")

        println("\n=== Example 4: Activate BRE-B Key ===")

        val activated = session.accounts.breb.activateKey(
            ActivateBrebKeyParams(accountUrn = account.urn)
        )

        println("Activate error: ${activated.error?.message}")
        println("Activate status: ${activated.data?.status}")
        println("Upstream key status: ${activated.data?.keyStatus}")

        println("\n=== Example 5: Delete BRE-B Key ===")

        val deleted = session.accounts.breb.deleteKey(
            DeleteBrebKeyParams(accountUrn = account.urn)
        )

        println("Delete error: ${deleted.error?.message}")
        println("Deleted: ${deleted.data?.deleted}")
        println("Delete status: ${deleted.data?.status}")
    }

    println("\n=== All BRE-B Examples Completed ===")
}
