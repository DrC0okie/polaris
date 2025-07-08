package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.ISharedKeyManager
import ch.heigvd.iict.services.crypto.LibsodiumBridge
import ch.heigvd.iict.services.crypto.model.*
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import jakarta.enterprise.context.ApplicationScoped

/**
 * Concrete implementation of [IMessageSealer] using ChaCha20-Poly1305 AEAD.
 *
 * This service handles the process of encrypting a plaintext message for a specific beacon,
 * including key derivation, nonce generation, and serialization into a transportable format.
 *
 * @property keyManager The manager used to retrieve the correct shared secret for the target beacon.
 */
@ApplicationScoped
class AeadMessageSealer(private val keyManager: ISharedKeyManager) : IMessageSealer {

    /**
     * Seals a [PlaintextMessage] using AEAD encryption.
     *
     * @param plaintext The message to encrypt.
     * @param targetBeacon The destination beacon, used to fetch the shared encryption key.
     * @return A [SealedMessage] containing the resulting ciphertext and metadata.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun seal(plaintext: PlaintextMessage, targetBeacon: Beacon): SealedMessage {
        val sharedKey = keyManager.getSharedKeyForBeacon(targetBeacon)
        val nonce = LibsodiumBridge.generateNonce(12) // 12 bytes for IETF
        val associatedData = targetBeacon.beaconId.toUInt().toUByteArrayLE()

        val ciphertextWithTag = LibsodiumBridge.aeadEncrypt(
            message = plaintext.toBytes(),
            associatedData = associatedData,
            nonce = nonce,
            key = sharedKey.asUByteArray()
        )

        return SealedMessage(targetBeacon.beaconId, nonce, ciphertextWithTag)
    }
}