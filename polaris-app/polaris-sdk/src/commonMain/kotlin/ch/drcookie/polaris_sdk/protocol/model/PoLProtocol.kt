package ch.drcookie.polaris_sdk.protocol.model

/**
 * These constants represent the sizes of every PoL message composant
 */
internal object Constants {
    const val ED25519_PK = 32
    const val SIG = 64
    const val PROTOCOL_NONCE = 16
    const val PHONE_ID = 8
    const val FLAGS = 1
    const val BEACON_ID = 4
    const val BEACON_COUNTER = 8
}

@OptIn(ExperimentalUnsignedTypes::class)
public data class PoLRequest(
    public val flags: UByte,
    public val phoneId: ULong,
    public val beaconId: UInt,
    public val nonce: UByteArray, // Size: PoLConstants.PROTOCOL_NONCE_SIZE
    public val phonePk: UByteArray, // Size: PoLConstants.ED25519_PK_SIZE
    public var phoneSig: UByteArray? = null, // Size: PoLConstants.SIG_SIZE, nullable until signed
) {
    init {
        require(nonce.size == Constants.PROTOCOL_NONCE) { "Invalid nonce size" }
        require(phonePk.size == Constants.ED25519_PK) { "Invalid phone PK size" }
        phoneSig?.let { require(it.size == Constants.SIG) { "Invalid signature size" } }
    }

    public companion object {
        /**
         * Size of the PoLRequest data tha has been signed
         */
        public const val SIGNED_DATA_SIZE: Int =
            Constants.FLAGS + // 1
            Constants.PHONE_ID + // 8
            Constants.BEACON_ID + // 4
            Constants.PROTOCOL_NONCE + // 16
            Constants.ED25519_PK // 32

        /**
         * Total size of a PoLRequest message
         */
        public const val PACKED_SIZE: Int = SIGNED_DATA_SIZE + Constants.SIG // 125 bytes
    }

    public override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + phoneId.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + phonePk.contentHashCode()
        result = 31 * result + (phoneSig?.contentHashCode() ?: 0)
        return result
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PoLRequest
        if (flags != other.flags) return false
        if (phoneId != other.phoneId) return false
        if (beaconId != other.beaconId) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!phonePk.contentEquals(other.phonePk)) return false
        if (!phoneSig.contentEquals(other.phoneSig)) return false
        return true
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
public data class PoLResponse(
    val flags: UByte,
    val beaconId: UInt,
    val counter: ULong,
    val nonce: UByteArray, // Size: PoLConstants.PROTOCOL_NONCE_SIZE
    val beaconSig: UByteArray, // Size: PoLConstants.SIG_SIZE
) {
    init {
        require(nonce.size == Constants.PROTOCOL_NONCE) { "Invalid nonce size" }
        require(beaconSig.size == Constants.SIG) { "Invalid signature size" }
    }

    public companion object {

        /**
         * Size of the data that was signed by the beacon (includes request context)
         */
        public const val EFFECTIVE_SIGNED_DATA_SIZE: Int =
            Constants.FLAGS + // 1
            Constants.BEACON_ID + // 4
            Constants.BEACON_COUNTER + // 8
            Constants.PROTOCOL_NONCE + // 16
            Constants.PHONE_ID + // 8
            Constants.ED25519_PK + // 32
            Constants.SIG // 64

        /**
         * Total size of the PoLResponse message
         */
        public const val PACKED_SIZE: Int =
            Constants.FLAGS + // 1
            Constants.BEACON_ID + // 4
            Constants.BEACON_COUNTER + // 8
            Constants.PROTOCOL_NONCE + // 16
            Constants.SIG // 64
    }

    public override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PoLResponse
        if (flags != other.flags) return false
        if (beaconId != other.beaconId) return false
        if (counter != other.counter) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!beaconSig.contentEquals(other.beaconSig)) return false
        return true
    }
}