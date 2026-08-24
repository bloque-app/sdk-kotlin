package app.bloque.sdk.compliance

import app.bloque.sdk.core.BaseClient
import app.bloque.sdk.core.BloqueHttpClient

private fun mapAcceptance(wire: TosAcceptanceRecordWire): TosAcceptanceRecord {
    return TosAcceptanceRecord(
        id = wire.id,
        identityUrn = wire.identityUrn,
        documentVersionId = wire.documentVersionId,
        documentVersionLabel = wire.documentVersionLabel,
        documentHash = wire.documentHash,
        acceptedAt = wire.acceptedAt,
        authAssurance = wire.authAssurance
    )
}

private fun mapPasskeyChallenge(wire: TosGatePasskeyChallengeWire?): TosGatePasskeyChallenge? {
    if (wire == null) return null
    return TosGatePasskeyChallenge(
        challenge = wire.challenge,
        context = wire.context,
        expiresAtBlock = wire.expiresAtBlock,
        userId = wire.userId,
        userName = wire.userName,
        publicAddress = wire.publicAddress
    )
}

private fun toAcceptRequestWire(params: TosGateAcceptParams): TosGateAcceptRequestWire {
    return when (val attestation = params.attestation) {
        is TosGateAttestation.DeviceAttestation -> TosGateAcceptRequestWire(
            csrfToken = params.csrfToken,
            deviceAttestation = attestation.hex
        )
        is TosGateAttestation.Passkey -> TosGateAcceptRequestWire(
            csrfToken = params.csrfToken,
            passkey = TosGatePasskeyRegistrationWire(
                credentialId = attestation.registration.credentialId,
                authenticatorData = attestation.registration.authenticatorData,
                clientData = attestation.registration.clientData,
                publicKey = attestation.registration.publicKey,
                context = attestation.registration.context
            )
        )
        null -> TosGateAcceptRequestWire(csrfToken = params.csrfToken)
    }
}

/**
 * Level 0 TOS gate (`/api/tos-gate` routes) — the hosted page a user opens to
 * accept the Terms of Service.
 *
 * [start] authenticates as the SDK's connected session, same as any other
 * call. [init]/[accept] authenticate solely via the capability `token`
 * [start] returns — the same bearer credential the hosted page itself
 * uses — so they work without a live session for that identity. This is
 * what makes the returned `url` portable to any browser.
 *
 * Usually you won't call these directly: catch a
 * `BloqueVerificationRequiredError` with `reason == "tos"` and call its
 * `getVerificationLink()` instead, which calls [start] for you.
 */
class TosGateClient constructor(
    httpClient: BloqueHttpClient
) : BaseClient(httpClient) {

    /**
     * Mint a portable TOS gate capability token + hosted page URL.
     */
    fun start(params: StartTosGateParams): StartGateResult {
        val response = httpClient.post<GateStartResponseWire, GateStartRequestWire>(
            path = "/api/tos-gate/start",
            body = GateStartRequestWire(returnUrl = params.returnUrl)
        )
        return StartGateResult(token = response.token, url = response.url, expiresIn = response.expiresIn)
    }

    /**
     * Fetch the active TOS document for the token's identity and mint a
     * single-use acceptance nonce. Authorized solely by [params]'s token.
     */
    fun init(params: TosGateInitParams): TosGateInitResult {
        val response = httpClient.get<TosGateInitResponseWire>(
            path = "/api/tos-gate/init",
            headers = mapOf("Authorization" to "Bearer ${params.token}")
        )
        return TosGateInitResult(
            document = TosGateDocument(
                documentVersionId = response.document.documentVersionId,
                versionLabel = response.document.versionLabel,
                contentHash = response.document.contentHash,
                content = response.document.content
            ),
            csrfToken = response.csrfToken,
            returnUrl = response.returnUrl,
            showHome = response.showHome,
            accentColor = response.accentColor,
            developerName = response.developerName,
            passkeyRequired = response.passkeyRequired
        )
    }

    /**
     * Mint a WebAuthn registration challenge for the token's identity, for
     * the passkey step [TosGateInitResult.passkeyRequired] gates. Called at
     * the moment the user actually commits (not at page load) — the
     * challenge is bound to a chain block with a limited answer window, so
     * minting it early would spend that window on however long the user
     * spends reading the document. Authorized solely by [params]'s token.
     *
     * @return the challenge, or `null` if this identity has no account ready for a device
     */
    fun challenge(params: TosGateInitParams): TosGateChallengeResult {
        val response = httpClient.get<TosGateChallengeResponseWire>(
            path = "/api/tos-gate/challenge",
            headers = mapOf("Authorization" to "Bearer ${params.token}")
        )
        return TosGateChallengeResult(passkey = mapPasskeyChallenge(response.passkey))
    }

    /**
     * Record TOS acceptance for the token's identity. Authorized solely by
     * [params]'s token; requires the single-use `csrfToken` from [init].
     */
    fun accept(params: TosGateAcceptParams): TosGateAcceptResult {
        val body = toAcceptRequestWire(params)
        val response = httpClient.post<TosGateAcceptResponseWire, TosGateAcceptRequestWire>(
            path = "/api/tos-gate/accept",
            body = body,
            headers = mapOf("Authorization" to "Bearer ${params.token}")
        )
        return TosGateAcceptResult(
            acceptance = mapAcceptance(response.acceptance),
            returnUrl = response.returnUrl
        )
    }
}
