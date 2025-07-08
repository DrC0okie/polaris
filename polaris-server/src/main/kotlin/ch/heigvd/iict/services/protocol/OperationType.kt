package ch.heigvd.iict.services.protocol

/**
 * Defines the specific commands or operations within the secure communication protocol.
 *
 * @property code The 1-byte code used to represent the operation in the serialized message.
 */
enum class OperationType(val code: UByte) {
    /** A no-operation command, typically used as a ping or keep-alive. */
    NO_OP(0x00u),
    /** Command to make the beacon reboot itself. */
    REBOOT(0x01u),
    /** Command to make the beacon's LED blink. */
    BLINK_LED(0x02u),
    /** Command to stop the LED blinking. */
    STOP_BLINK(0x03u),
    /** Command to display text on the beacon's screen. */
    DISPLAY_TEXT(0x04u),
    /** Command to clear the beacon's screen. */
    CLEAR_DISPLAY(0x05u),
    /** A request from the server for the beacon to report its status. */
    REQUEST_BEACON_STATUS(0x06u),
    /** The first step of the key rotation sequence, initiated by the server. */
    ROTATE_KEY_INIT(0x10u),
    /** The final step of the key rotation, confirming completion. */
    ROTATE_KEY_FINISH(0x11u),
    /** A response from a beacon containing its status, sent after a [REQUEST_BEACON_STATUS]. */
    RESPONSE_BEACON_STATUS(0x80u),
    /** An unknown or unsupported operation type. */
    UNKNOWN(0xFFu);

    companion object {
        /** Converts a byte code back to its corresponding [OperationType]. */
        fun fromCode(code: UByte): OperationType = entries.find { it.code == code }?:UNKNOWN
    }
}