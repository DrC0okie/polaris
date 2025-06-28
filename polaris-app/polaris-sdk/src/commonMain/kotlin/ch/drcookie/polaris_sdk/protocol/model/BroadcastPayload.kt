package ch.drcookie.polaris_sdk.protocol.model

/**
 * Structure of a non-connectable extended advertisement broadcast.
 *
 * Designed for passive monitoring, allowing to prove its presence without requiring a BLE connection.
 *
 * @property beaconId Unique identifier of the beacon sending the broadcast.
 * @property counter Monotonic counter from the beacon.
 * @property signature Ed25519 signature of the concatenated [beaconId] and [counter]
 */
@OptIn(ExperimentalUnsignedTypes::class)
public data class BroadcastPayload(
    public val beaconId: UInt,
    public val counter: ULong,
    public val signature: UByteArray
) {
    init {
        // Enforce size constraint for the signature
        require(signature.size == Constants.SIG) {
            "Invalid signature size. Expected ${Constants.SIG}, got ${signature.size}"
        }
    }

    public companion object {
        /**
         * The total packed size in bytes of a serialized [BroadcastPayload].
         * This is useful for pre-validating the length of raw advertisement data.
         */
        public const val PACKED_SIZE: Int = Constants.BEACON_ID + Constants.BEACON_COUNTER + Constants.SIG
    }

    public override fun hashCode(): Int {
        var result = beaconId.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BroadcastPayload

        if (beaconId != other.beaconId) return false
        if (counter != other.counter) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }
}