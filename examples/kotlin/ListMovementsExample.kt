package examples.kotlin

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.UserSession
import app.bloque.sdk.accounts.ListMovementsParams

fun main() {
    val bloque = BloqueSDK.builder()
        .origin("bloque-root")
        .apiKey("sk_test_mock_api_key")
        .mode(Mode.SANDBOX)
        .build()

    val session: UserSession = bloque.connect("mock-user")

    val accountUrn = "did:bloque:account:card:usr-mockUser:crd-mockCard"

    // ============================================
    // Example 1: List movements (paginated)
    // ============================================
    println("=== Standard movements (paginated) ===")

    val page1 = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            limit = 10,
            direction = "in"
        )
    )

    println("Page size: ${page1.pageSize}")
    println("Has more: ${page1.hasMore}")
    println("Movements:")
    page1.data.forEach { m ->
        println("  ${m.status} | ${m.direction} | ${m.amount} ${m.asset} | ref=${m.reference}")
    }

    // Fetch next page if available
    if (page1.hasMore && page1.next != null) {
        println("\n=== Fetching next page ===")
        val page2 = session.accounts.movements(
            ListMovementsParams(
                urn = accountUrn,
                asset = "KSM/12",
                next = page1.next
            )
        )
        println("Page 2 size: ${page2.pageSize}")
        page2.data.forEach { m ->
            println("  ${m.status} | ${m.direction} | ${m.amount} ${m.asset}")
        }
    }

    // ============================================
    // Example 2: Filter by pocket (confirmed only)
    // ============================================
    println("\n=== Confirmed movements only (pocket=main) ===")

    val confirmed = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            pocket = "main"
        )
    )

    confirmed.data.forEach { m ->
        println("  ${m.status} | ${m.amount} ${m.asset} | ${m.createdAt}")
    }

    // ============================================
    // Example 3: Pending movements only
    // ============================================
    println("\n=== Pending movements only (pocket=pending) ===")

    val pending = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            pocket = "pending"
        )
    )

    pending.data.forEach { m ->
        println("  ${m.status} | ${m.amount} ${m.asset} | ${m.createdAt}")
    }

    // ============================================
    // Example 4: Collapsed view
    // ============================================
    println("\n=== Collapsed view (pending + confirmed merged) ===")

    val collapsed = session.accounts.movements(
        ListMovementsParams(
            urn = accountUrn,
            asset = "KSM/12",
            collapsedView = true
        )
    )

    collapsed.data.forEach { m ->
        println("  ${m.status} | ${m.direction} | ${m.amount} ${m.asset} | ref=${m.reference}")
    }
}
