package ch.drcookie.polaris_sdk.domain.model

import ch.drcookie.polaris_sdk.core.ByteConversionUtils.toUByteArrayLE
import ch.drcookie.polaris_sdk.core.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.core.ByteConversionUtils.toULongLE

@OptIn(ExperimentalUnsignedTypes::class)
data class PoLRequest(
    val flags: UByte,
    val phoneId: ULong,
    val beaconId: UInt,
    val nonce: UByteArray, // Size: PoLConstants.PROTOCOL_NONCE_SIZE
    val phonePk: UByteArray, // Size: PoLConstants.ED25519_PK_SIZE
    var phoneSig: UByteArray? = null // Size: PoLConstants.SIG_SIZE, nullable until signed
) {
    init {
        require(nonce.size == Constants.PROTOCOL_NONCE_SIZE) { "Invalid nonce size" }
        require(phonePk.size == Constants.ED25519_PK_SIZE) { "Invalid phone PK size" }
        phoneSig?.let { require(it.size == Constants.SIG_SIZE) { "Invalid signature size" } }
    }

    companion object {
        const val SIGNED_DATA_SIZE = 1 /*flags*/ + 8 /*phoneId*/ + 4 /*beaconId*/ +
                Constants.PROTOCOL_NONCE_SIZE + Constants.ED25519_PK_SIZE

        const val PACKED_SIZE = SIGNED_DATA_SIZE + Constants.SIG_SIZE

        fun fromBytes(data: ByteArray): PoLRequest? {
            if (data.size < PACKED_SIZE) return null
            var offset = 0
            val uData = data.toUByteArray() // Work with UByteArrays for unsigned values

            val flags = uData[offset]; offset += 1
            val phoneId = uData.sliceArray(offset until offset + 8).toULongLE()
            offset += 8
            val beaconId = uData.sliceArray(offset until offset + 4).toUIntLE()
            offset += 4
            val nonce = uData.sliceArray(offset until offset + Constants.PROTOCOL_NONCE_SIZE)
            offset += Constants.PROTOCOL_NONCE_SIZE
            val phonePk = uData.sliceArray(offset until offset + Constants.ED25519_PK_SIZE)
            offset += Constants.ED25519_PK_SIZE
            val phoneSig = uData.sliceArray(offset until offset + Constants.SIG_SIZE)
            offset += Constants.SIG_SIZE

            return PoLRequest(flags, phoneId, beaconId, nonce, phonePk, phoneSig)
        }
    }

    fun getSignedData(): UByteArray {
        val buffer = UByteArray(SIGNED_DATA_SIZE)
        var offset = 0
        buffer[offset] = flags
        offset += 1
        phoneId.toUByteArrayLE().copyInto(buffer, offset)
        offset += 8
        beaconId.toUByteArrayLE().copyInto(buffer, offset)
        offset += 4
        nonce.copyInto(buffer, offset)
        offset += Constants.PROTOCOL_NONCE_SIZE
        phonePk.copyInto(buffer, offset) // offset += PoLConstants.ED25519_PK_SIZE
        return buffer
    }

    fun toBytes(): ByteArray {
        val sig = phoneSig ?: throw IllegalStateException("Signature not set")
        val buffer = UByteArray(PACKED_SIZE)
        var offset = 0

        buffer[offset] = flags; offset += 1
        phoneId.toUByteArrayLE().copyInto(buffer, offset)
        offset += 8
        beaconId.toUByteArrayLE().copyInto(buffer, offset)
        offset += 4
        nonce.copyInto(buffer, offset)
        offset += Constants.PROTOCOL_NONCE_SIZE
        phonePk.copyInto(buffer, offset)
        offset += Constants.ED25519_PK_SIZE
        sig.copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

        return buffer.asByteArray()
    }

    override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + phoneId.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + phonePk.contentHashCode()
        result = 31 * result + (phoneSig?.contentHashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
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
data class PoLResponse(
    val flags: UByte,
    val beaconId: UInt,
    val counter: ULong,
    val nonce: UByteArray, // Size: PoLConstants.PROTOCOL_NONCE_SIZE
    val beaconSig: UByteArray // Size: PoLConstants.SIG_SIZE
) {
    init {
        require(nonce.size == Constants.PROTOCOL_NONCE_SIZE) { "Invalid nonce size" }
        require(beaconSig.size == Constants.SIG_SIZE) { "Invalid signature size" }
    }

    companion object {
        // Data that WAS signed by the beacon (includes request context)
        const val EFFECTIVE_SIGNED_DATA_SIZE = 1 /*flags*/ + 4 /*beaconId*/ + 8 /*counter*/ +
                Constants.PROTOCOL_NONCE_SIZE +
                8 /*req.phoneId*/ + Constants.ED25519_PK_SIZE /*req.phonePk*/ +
                Constants.SIG_SIZE /*req.phoneSig*/

        const val PACKED_SIZE = 1 /*flags*/ + 4 /*beaconId*/ + 8 /*counter*/ +
                Constants.PROTOCOL_NONCE_SIZE + Constants.SIG_SIZE

        fun fromBytes(data: ByteArray): PoLResponse? {
            if (data.size < PACKED_SIZE) return null
            var offset = 0
            val uData = data.toUByteArray()

            val flags = uData[offset]; offset += 1
            val beaconId = uData.sliceArray(offset until offset + 4).toUIntLE(); offset += 4
            val counter = uData.sliceArray(offset until offset + 8).toULongLE(); offset += 8
            val nonce = uData.sliceArray(offset until offset + Constants.PROTOCOL_NONCE_SIZE); offset += Constants.PROTOCOL_NONCE_SIZE
            val beaconSig = uData.sliceArray(offset until offset + Constants.SIG_SIZE) // offset += PoLConstants.SIG_SIZE

            return PoLResponse(flags, beaconId, counter, nonce, beaconSig)
        }
    }

    // Data that is physically sent over BLE
    fun toBytes(): ByteArray {
        val buffer = UByteArray(PACKED_SIZE)
        var offset = 0
        buffer[offset] = flags; offset += 1
        beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
        counter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        nonce.copyInto(buffer, offset); offset += Constants.PROTOCOL_NONCE_SIZE
        beaconSig.copyInto(buffer, offset)
        return buffer.asByteArray()
    }

    // Constructs the data that the beacon *actually* signed
    fun getEffectivelySignedData(originalRequest: PoLRequest): UByteArray {
        val buffer = UByteArray(EFFECTIVE_SIGNED_DATA_SIZE)
        var offset = 0

        // Response fields
        buffer[offset] = flags; offset += 1
        beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
        counter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        nonce.copyInto(buffer, offset); offset += Constants.PROTOCOL_NONCE_SIZE // This is originalRequest.nonce

        // Original Request fields
        originalRequest.phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        originalRequest.phonePk.copyInto(buffer, offset); offset += Constants.ED25519_PK_SIZE
        (originalRequest.phoneSig ?: UByteArray(Constants.SIG_SIZE)) // Use placeholder if null, though it should be set
            .copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

        return buffer
    }

    override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
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