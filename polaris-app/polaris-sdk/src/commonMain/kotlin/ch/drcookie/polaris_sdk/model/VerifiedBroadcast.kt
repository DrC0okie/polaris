package ch.drcookie.polaris_sdk.model

import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload

data class VerifiedBroadcast(
    val payload: BroadcastPayload,
    val isSignatureValid: Boolean
)