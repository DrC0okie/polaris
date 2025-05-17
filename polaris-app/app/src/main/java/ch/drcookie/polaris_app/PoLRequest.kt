package ch.drcookie.polaris_app

import ch.drcookie.polaris_app.Utils.toUByteArrayLE
import ch.drcookie.polaris_app.Utils.toUIntLE
import ch.drcookie.polaris_app.Utils.toULongLE

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
        require(nonce.size == PoLConstants.PROTOCOL_NONCE_SIZE) { "Invalid nonce size" }
        require(phonePk.size == PoLConstants.ED25519_PK_SIZE) { "Invalid phone PK size" }
        phoneSig?.let { require(it.size == PoLConstants.SIG_SIZE) { "Invalid signature size" } }
    }

    companion object {
        const val SIGNED_DATA_SIZE = 1 /*flags*/ + 8 /*phoneId*/ + 4 /*beaconId*/ +
                PoLConstants.PROTOCOL_NONCE_SIZE + PoLConstants.ED25519_PK_SIZE

        const val PACKED_SIZE = SIGNED_DATA_SIZE + PoLConstants.SIG_SIZE

        fun fromBytes(data: ByteArray): PoLRequest? {
            if (data.size < PACKED_SIZE) return null
            var offset = 0
            val uData = data.toUByteArray() // Work with UByteArrays for unsigned values

            val flags = uData[offset]; offset += 1
            val phoneId = uData.sliceArray(offset until offset + 8).toULongLE()
            offset += 8
            val beaconId = uData.sliceArray(offset until offset + 4).toUIntLE()
            offset += 4
            val nonce = uData.sliceArray(offset until offset + PoLConstants.PROTOCOL_NONCE_SIZE)
            offset += PoLConstants.PROTOCOL_NONCE_SIZE
            val phonePk = uData.sliceArray(offset until offset + PoLConstants.ED25519_PK_SIZE)
            offset += PoLConstants.ED25519_PK_SIZE
            val phoneSig = uData.sliceArray(offset until offset + PoLConstants.SIG_SIZE)
            offset += PoLConstants.SIG_SIZE

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
        offset += PoLConstants.PROTOCOL_NONCE_SIZE
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
        offset += PoLConstants.PROTOCOL_NONCE_SIZE
        phonePk.copyInto(buffer, offset)
        offset += PoLConstants.ED25519_PK_SIZE
        sig.copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

        return buffer.asByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PoLRequest

        if (flags != other.flags) return false
        if (phoneId != other.phoneId) return false
        if (beaconId != other.beaconId) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!phonePk.contentEquals(other.phonePk)) return false
        if (!phoneSig.contentEquals(other.phoneSig)) return false

        return true
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
}
