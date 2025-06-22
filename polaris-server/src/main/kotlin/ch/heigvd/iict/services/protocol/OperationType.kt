package ch.heigvd.iict.services.protocol

enum class OperationType(val code: UByte) {
    NO_OP(0x00u),
    REBOOT(0x01u),
    BLINK_LED(0x02u),
    STOP_BLINK(0x03u),
    DISPLAY_TEXT(0x04u),
    CLEAR_DISPLAY(0x05u),
    UNKNOWN(0xFFu);

    companion object {
        fun fromCode(code: UByte): OperationType = entries.find { it.code == code }?:UNKNOWN
    }
}