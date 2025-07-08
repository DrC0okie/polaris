package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.model.*

/**
 * Defines the contract for a service that can decrypt a [SealedMessage].
 *
 * This interface abstracts the details of the AEAD decryption and verification process.
 */
interface IMessageUnsealer {

    /**
     * Decrypts and verifies a sealed message, returning the original plaintext.
     *
     * @param sealed The [SealedMessage] received from a mobile client.
     * @param sourceBeacon The [Beacon] that originated the message, used to derive the correct shared key.
     * @return The original [PlaintextMessage] if decryption and authentication are successful.
     * @throws com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException if the message is invalid.
     */
    fun unseal(sealed: SealedMessage, sourceBeacon: Beacon): PlaintextMessage
}