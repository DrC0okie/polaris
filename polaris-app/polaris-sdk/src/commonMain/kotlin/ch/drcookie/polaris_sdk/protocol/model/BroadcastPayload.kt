package ch.drcookie.polaris_sdk.protocol.model

/**
 * Represents the data structure of a non-connectable extended advertisement broadcast.
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
        // Total size of the payload: 4 (id) + 8 (counter) + 64 (sig) = 76 bytes
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