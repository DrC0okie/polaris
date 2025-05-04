package ch.drcookie.polaris_app

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PoLResponse(
    val flags: Byte,
    val beaconId: Int,
    val counter: Long,
    val nonce: ByteArray,
    val beaconSignature: ByteArray
) {
    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(packedSize()).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(flags)
        buffer.putInt(beaconId)
        buffer.putLong(counter)
        buffer.put(nonce)
        buffer.put(beaconSignature)
        return buffer.array()
    }

    fun getSignedData(): ByteArray {
        val buffer = ByteBuffer.allocate(SIGNED_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(flags)
        buffer.putInt(beaconId)
        buffer.putLong(counter)
        buffer.put(nonce)
        return buffer.array()
    }

    companion object {
        const val NONCE_SIZE = 16
        const val SIG_SIZE = 64
        const val SIGNED_SIZE = 1 + 4 + 8 + NONCE_SIZE
        const val TOTAL_SIZE = SIGNED_SIZE + SIG_SIZE

        fun fromBytes(data: ByteArray): PoLResponse? {
            if (data.size != TOTAL_SIZE) return null
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val flags = buffer.get()
            val beaconId = buffer.int
            val counter = buffer.long
            val nonce = ByteArray(NONCE_SIZE).also { buffer.get(it) }
            val signature = ByteArray(SIG_SIZE).also { buffer.get(it) }

            return PoLResponse(flags, beaconId, counter, nonce, signature)
        }

        fun packedSize() = TOTAL_SIZE
    }
}

