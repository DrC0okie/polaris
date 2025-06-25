package ch.drcookie.polaris_sdk.model

import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload

public data class VerifiedBroadcast(
    public val payload: BroadcastPayload,
    public val isSignatureValid: Boolean
)