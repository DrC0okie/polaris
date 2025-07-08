package ch.heigvd.iict.services.protocol

/**
 * Defines the types of messages in the secure communication protocol.
 *
 * @property code The 1-byte code used to represent the type in the serialized message.
 */
enum class MessageType(val code: UByte) {
    /** An undefined or unrecognized message type. */
    UNDEFINED(0x00u),
    /** A request message, initiating an action. */
    REQ(0x01u),
    /** An acknowledgment message, confirming receipt or success of a request. */
    ACK(0x02u),
    /** An error message, indicating a failure in processing a request. */
    ERR(0x03u);

    companion object {
        /** Converts a byte code back to its corresponding [MessageType]. */
        fun fromCode(code: UByte): MessageType = entries.find { it.code == code }?:UNDEFINED
    }
}