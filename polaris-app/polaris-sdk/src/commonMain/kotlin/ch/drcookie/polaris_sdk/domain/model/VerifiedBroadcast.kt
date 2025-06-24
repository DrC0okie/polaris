package ch.drcookie.polaris_sdk.domain.model

data class VerifiedBroadcast(
    val payload: BroadcastPayload,
    val isSignatureValid: Boolean
)
