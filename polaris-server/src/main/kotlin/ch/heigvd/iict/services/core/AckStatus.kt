package ch.heigvd.iict.services.core

enum class AckStatus {
    PENDING_ACK,
    ACK_RECEIVED,
    ERR_RECEIVED,
    EXPIRED
}