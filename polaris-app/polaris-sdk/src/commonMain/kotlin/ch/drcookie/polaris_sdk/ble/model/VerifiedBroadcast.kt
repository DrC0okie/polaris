package ch.drcookie.polaris_sdk.ble.model

import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload

/**
 * Wraps a received [BroadcastPayload] with the result of its signature verification.
 *
 * @property payload The original [BroadcastPayload] parsed from the BLE advertisement.
 * @property isSignatureValid `true` if the payload's signature was verified, `false` otherwise.
 */
public data class VerifiedBroadcast(
    public val payload: BroadcastPayload,
    public val isSignatureValid: Boolean
)