package ch.heigvd.iict.services.protocol

import ch.heigvd.iict.entities.OutboundMessage

/**
 * Enumerates the lifecycle states of an [OutboundMessage] job.
 */
enum class MessageStatus {
    /** The message is created and ready to be picked up by a phone. */
    PENDING,
    /** The message has been picked up by at least one phone but has not yet been acknowledged. */
    DELIVERING,
    /** The server has received the first successful acknowledgment from the beacon for this message. */
    ACKNOWLEDGED,
    /** The message processing has failed (e.g., due to a processing error or an ERR from the beacon). */
    FAILED,
    /** The message was not acknowledged within a defined timeframe and is considered lost. */
    TIMED_OUT,
    /** An acknowledgment was received for a job that was already completed (acknowledged or failed). */
    REDUNDANT
}