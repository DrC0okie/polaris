package ch.drcookie.polaris_app

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PoLRequest(
    val flags: Byte,
    val phoneId: Long,
    val beaconId: Int,
    val nonce: ByteArray,
    val phonePublicKey: ByteArray,
    val phoneSignature: ByteArray
) {
    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(packedSize()).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(flags)
        buffer.putLong(phoneId)
        buffer.putInt(beaconId)
        buffer.put(nonce)
        buffer.put(phonePublicKey)
        buffer.put(phoneSignature)
        return buffer.array()
    }

    fun getSignedData(): ByteArray {
        val buffer = ByteBuffer.allocate(SIGNED_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(flags)
        buffer.putLong(phoneId)
        buffer.putInt(beaconId)
        buffer.put(nonce)
        buffer.put(phonePublicKey)
        return buffer.array()
    }

    companion object {
        const val NONCE_SIZE = 16
        const val SIG_SIZE = 64
        const val PK_SIZE = 32
        const val SIGNED_SIZE = 1 + 8 + 4 + NONCE_SIZE + PK_SIZE
        const val TOTAL_SIZE = SIGNED_SIZE + SIG_SIZE

        fun fromBytes(data: ByteArray): PoLRequest? {
            if (data.size != TOTAL_SIZE) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val flags = buffer.get()
            val phoneId = buffer.long
            val beaconId = buffer.int
            val nonce = ByteArray(NONCE_SIZE).also { buffer.get(it) }
            val phonePk = ByteArray(PK_SIZE).also { buffer.get(it) }
            val signature = ByteArray(SIG_SIZE).also { buffer.get(it) }

            return PoLRequest(flags, phoneId, beaconId, nonce, phonePk, signature)
        }

        fun packedSize() = TOTAL_SIZE
    }
}