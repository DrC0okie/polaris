package ch.heigvd.iict.services.crypto.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A data class representing a complete, encrypted message ready for transport.
 *
 * This structure includes both the unencrypted metadata needed for decryption (associated data)
 * and the encrypted payload itself. Its binary format is designed to match the `EncryptedMessage`
 * structure on the beacon firmware.
 *
 * @property beaconId The ID of the beacon, used as Associated Data in the AEAD operation.
 * @property nonce The 12-byte nonce used for this specific encryption. Must be unique per message.
 * @property ciphertextWithTag The combined ciphertext and authentication tag produced by the AEAD algorithm.
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class SealedMessage(
    val beaconId: Int,
    val nonce: UByteArray, // 12 bytes for IETF
    val ciphertextWithTag: UByteArray
) {
    /**
     * Serializes this object into a single byte array (a "blob") for transmission.
     * The format is `beaconId (4 bytes) || nonce (12 bytes) || ciphertext`.
     * @return The serialized message as a [ByteArray].
     */
    fun toBlob(): ByteArray {
        val buffer = ByteBuffer.allocate(4 + 12 + ciphertextWithTag.size)
            .order(ByteOrder.LITTLE_ENDIAN) // ESP32 works with little endian
        buffer.putInt(beaconId)
        buffer.put(nonce.asByteArray())
        buffer.put(ciphertextWithTag.asByteArray())
        return buffer.array()
    }

    companion object {

        /**
         * Deserializes a raw byte blob into a [SealedMessage] object.
         * @param blob The raw byte array received from a mobile client.
         * @return A new [SealedMessage] instance.
         */
        fun fromBlob(blob: ByteArray): SealedMessage {
            val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
            val beaconId = buffer.int
            val nonce = UByteArray(12).apply { buffer.get(this.asByteArray()) }
            val ciphertext = UByteArray(buffer.remaining()).apply { buffer.get(this.asByteArray()) }
            return SealedMessage(beaconId, nonce, ciphertext)
        }
    }
}