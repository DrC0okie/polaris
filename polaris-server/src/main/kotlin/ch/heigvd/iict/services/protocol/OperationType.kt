package ch.heigvd.iict.services.protocol

enum class OperationType(val code: UByte) {
    UNDEFINED(0x00u),
    GENERIC_COMMAND(0x01u),
    REBOOT(0x02u),
    GET_STATUS(0x03u);

    companion object {
        fun fromCode(code: UByte): OperationType = entries.find { it.code == code }?:UNDEFINED
    }
}