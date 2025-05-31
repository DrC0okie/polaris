package ch.heigvd.iict.services.core

import ch.heigvd.iict.services.core.PoLUtils.toUByteArrayLE
import ch.heigvd.iict.services.core.PoLUtils.toUIntLE
import ch.heigvd.iict.services.core.PoLUtils.toULongLE

@OptIn(ExperimentalUnsignedTypes::class)
data class PoLResponse(
    val flags: UByte,
    val beaconId: UInt,
    val counter: ULong,
    val nonce: UByteArray, // Size: PoLConstants.PROTOCOL_NONCE_SIZE
    val beaconSig: UByteArray // Size: PoLConstants.SIG_SIZE
) {
    init {
        require(nonce.size == PoLConstants.PROTOCOL_NONCE_SIZE) { "Invalid nonce size" }
        require(beaconSig.size == PoLConstants.SIG_SIZE) { "Invalid signature size" }
    }

    companion object {
        // Data that WAS signed by the beacon (includes request context)
        const val EFFECTIVE_SIGNED_DATA_SIZE = 1 /*flags*/ + 4 /*beaconId*/ + 8 /*counter*/ +
                PoLConstants.PROTOCOL_NONCE_SIZE +
                8 /*req.phoneId*/ + PoLConstants.ED25519_PK_SIZE /*req.phonePk*/ +
                PoLConstants.SIG_SIZE /*req.phoneSig*/

        const val PACKED_SIZE = 1 /*flags*/ + 4 /*beaconId*/ + 8 /*counter*/ +
                PoLConstants.PROTOCOL_NONCE_SIZE + PoLConstants.SIG_SIZE

        fun fromBytes(data: ByteArray): PoLResponse? {
            if (data.size < PACKED_SIZE) return null
            var offset = 0
            val uData = data.toUByteArray()

            val flags = uData[offset]; offset += 1
            val beaconId = uData.sliceArray(offset until offset + 4).toUIntLE(); offset += 4
            val counter = uData.sliceArray(offset until offset + 8).toULongLE(); offset += 8
            val nonce = uData.sliceArray(offset until offset + PoLConstants.PROTOCOL_NONCE_SIZE); offset += PoLConstants.PROTOCOL_NONCE_SIZE
            val beaconSig = uData.sliceArray(offset until offset + PoLConstants.SIG_SIZE); // offset += PoLConstants.SIG_SIZE

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
        nonce.copyInto(buffer, offset); offset += PoLConstants.PROTOCOL_NONCE_SIZE
        beaconSig.copyInto(buffer, offset);
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
        nonce.copyInto(buffer, offset); offset += PoLConstants.PROTOCOL_NONCE_SIZE // This is originalRequest.nonce

        // Original Request fields
        originalRequest.phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        originalRequest.phonePk.copyInto(buffer, offset); offset += PoLConstants.ED25519_PK_SIZE
        (originalRequest.phoneSig ?: UByteArray(PoLConstants.SIG_SIZE)) // Use placeholder if null, though it should be set
            .copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

        return buffer
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PoLResponse

        if (flags != other.flags) return false
        if (beaconId != other.beaconId) return false
        if (counter != other.counter) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!beaconSig.contentEquals(other.beaconSig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }
}

