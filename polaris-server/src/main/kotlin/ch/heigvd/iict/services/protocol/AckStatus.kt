package ch.heigvd.iict.services.protocol

enum class AckStatus {
    PENDING_ACK,
    ACK_RECEIVED,
    ERR_RECEIVED,
    EXPIRED,
    FAILED_DECRYPTION,
    PROCESSING_ERROR
}