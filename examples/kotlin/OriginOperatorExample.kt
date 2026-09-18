package app.bloque.examples

import app.bloque.sdk.BloqueSDK
import app.bloque.sdk.core.Mode
import app.bloque.sdk.identity.CreateApiKeyParams
import app.bloque.sdk.identity.ExchangeApiKeyParams
import app.bloque.sdk.orgs.CreateOriginParams

/**
 * Origin-operator credentials: create (or reuse) an org-controlled origin,
 * assume it, mint an org-owned bound key, optionally impersonate a user of
 * that origin.
 *
 * The operator grant is read-only and origin-scoped. It never includes
 * `*.any`, pay/create, or passkey-as-user. `createOrigin` is the
 * self-service way to bind a new origin to the org — ops can otherwise
 * bind an existing one directly (`PUT /orgs/{org_urn}/controlled-origins/{namespace}`).
 *
 * Placeholders only — replace with a real user session (connect/register
 * as the human operator) before running against sandbox.
 */
fun main() {
    val bloque = BloqueSDK.builder()
        .secretKey("sk_test_your_api_key_here")
        .mode(Mode.SANDBOX)
        .build()

    val session = bloque.connect()

    // Skip this if the org already controls "your-origin-namespace" via
    // the ops-only bind path — createOrigin is only needed the first time.
    val created = session.orgs.createOrigin(
        orgUrn = "did:bloque:orgs:your-org-urn",
        params = CreateOriginParams(namespace = "your-origin-namespace")
    )
    println("Origin created (store the API key once): ${created.originApiKey}")

    val assumed = session.orgs.assumeOrigin("your-origin-namespace")
    println("Assumed origin-operator JWT (expires in ${assumed.expiresIn}s)")

    val bound = session.identity.apiKeys.create(
        CreateApiKeyParams(
            name = "cs-readonly",
            scopes = listOf("identity.read.origin", "alias.find.origin"),
            domains = emptyList(),
            expiration = "never"
        )
    )
    println("Bound secret (store once): ${bound.secretKey}")

    val discovery = session.identity.apiKeys.exchange(
        ExchangeApiKeyParams(key = bound.secretKey)
    )
    println("Discovery token type: ${discovery.tokenType}")

    val impersonated = session.identity.apiKeys.exchange(
        ExchangeApiKeyParams(
            key = bound.secretKey,
            asIdentity = "did:bloque:your-origin-namespace:customer-alias"
        )
    )
    println("Impersonation JWT expires in ${impersonated.expiresIn}s")
}
