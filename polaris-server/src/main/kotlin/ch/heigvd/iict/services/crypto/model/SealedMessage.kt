package ch.heigvd.iict.services.crypto.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalUnsignedTypes::class)
data class SealedMessage(
    val beaconId: Int,
    val nonce: UByteArray, // 12 bytes for IETF
    val ciphertextWithTag: UByteArray
) {
    // This serialization must match the beacon's `EncryptedMessage::fromBytes`
    fun toBlob(): ByteArray {
        // beaconIdAd (4) + nonce (12) + ciphertext
        val buffer = ByteBuffer.allocate(4 + 12 + ciphertextWithTag.size)
            .order(ByteOrder.LITTLE_ENDIAN) // ESP32 works with little endian
        buffer.putInt(beaconId)
        buffer.put(nonce.asByteArray())
        buffer.put(ciphertextWithTag.asByteArray())
        return buffer.array()
    }

    companion object {
        fun fromBlob(blob: ByteArray): SealedMessage {
            val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
            val beaconId = buffer.int
            val nonce = UByteArray(12).apply { buffer.get(this.asByteArray()) }
            val ciphertext = UByteArray(buffer.remaining()).apply { buffer.get(this.asByteArray()) }
            return SealedMessage(beaconId, nonce, ciphertext)
        }
    }
}