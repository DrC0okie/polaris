package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.ISharedKeyManager
import ch.heigvd.iict.services.crypto.LibsodiumBridge
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import jakarta.enterprise.context.ApplicationScoped

/**
 * Concrete implementation of [IMessageUnsealer] using ChaCha20-Poly1305 AEAD.
 *
 * This service decrypts and verifies an encrypted message blob, returning the original plaintext.
 *
 * @property keyManager The manager used to retrieve the correct shared secret for the source beacon.
 */
@ApplicationScoped
class AeadMessageUnsealer(private val keyManager: ISharedKeyManager) : IMessageUnsealer {

    /**
     * Unseals a [SealedMessage] by decrypting and verifying its content.
     *
     * @param sealed The encrypted message to process.
     * @param sourceBeacon The beacon that originated the message, used to fetch the shared key.
     * @return The original [PlaintextMessage] if decryption is successful.
     * @throws com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException if verification fails.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun unseal(sealed: SealedMessage, sourceBeacon: Beacon): PlaintextMessage {
        val sharedKey = keyManager.getSharedKeyForBeacon(sourceBeacon)
        val associatedData = sourceBeacon.beaconId.toUInt().toUByteArrayLE()

        val plaintextBytes = LibsodiumBridge.aeadDecrypt(
            ciphertextWithTag = sealed.ciphertextWithTag,
            associatedData = associatedData,
            nonce = sealed.nonce,
            key = sharedKey.asUByteArray()
        ).asByteArray()

        return PlaintextMessage.fromBytes(plaintextBytes)
    }
}