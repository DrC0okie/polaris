package ch.heigvd.iict.services.core

enum class MessageStatus {
    PENDING,     // Ready to be picked up
    DELIVERING,  // Picked up by at least one phone, but not N phones yet
    ACKNOWLEDGED, // The first ACK from the beacon has been received
    FAILED,      // The message processing failed
    TIMED_OUT    // The message was not acknowledged within a certain timeframe
}