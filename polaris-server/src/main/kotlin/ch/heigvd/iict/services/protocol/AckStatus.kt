package ch.heigvd.iict.services.protocol

import ch.heigvd.iict.entities.MessageDelivery
/**
 * Possible states of an acknowledgment for a specific [MessageDelivery].
 */
enum class AckStatus {
    /** The server is still waiting for the phone to submit the acknowledgment blob. */
    PENDING_ACK,
    /** A valid, decryptable ACK was received from the beacon. */
    ACK_RECEIVED,
    /** A valid, decryptable ERR was received from the beacon. */
    ERR_RECEIVED,
    /** The delivery acknowledgment was not received within the expected timeframe. */
    EXPIRED,
    /** An acknowledgment blob was received, but it failed decryption or authentication. */
    FAILED_DECRYPTION,
    /** An acknowledgment blob was received and decrypted, but an error occurred during its processing. */
    PROCESSING_ERROR
}