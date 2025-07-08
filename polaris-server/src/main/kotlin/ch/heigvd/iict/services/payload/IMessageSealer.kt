package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.model.*

/**
 * Defines the contract for a service that can encrypt a [PlaintextMessage].
 *
 * This interface abstracts the details of the AEAD encryption process, including
 * key derivation, nonce generation, and serialization.
 */
interface IMessageSealer {

    /**
     * Encrypts and "seals" a plaintext message, creating a transport-ready [SealedMessage].
     *
     * @param plaintext The [PlaintextMessage] to be encrypted.
     * @param targetBeacon The [Beacon] to which the message is being sent, used to derive the correct shared key.
     * @return A [SealedMessage] containing the encrypted data and necessary metadata.
     */
    fun seal(plaintext: PlaintextMessage, targetBeacon: Beacon): SealedMessage
}