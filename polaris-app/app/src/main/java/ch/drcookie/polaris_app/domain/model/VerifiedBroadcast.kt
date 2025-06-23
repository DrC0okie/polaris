package ch.drcookie.polaris_app.domain.model

data class VerifiedBroadcast(
    val payload: BroadcastPayload,
    val isSignatureValid: Boolean
)
