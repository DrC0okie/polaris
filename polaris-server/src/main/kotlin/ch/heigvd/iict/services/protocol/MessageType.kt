package ch.heigvd.iict.services.protocol

enum class MessageType(val code: UByte) {
    UNDEFINED(0x00u),
    REQ(0x01u),
    ACK(0x02u),
    ERR(0x03u);

    companion object {
        fun fromCode(code: UByte): MessageType = entries.find { it.code == code }?:UNDEFINED
    }
}