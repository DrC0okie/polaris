package ch.heigvd.iict.web.demo

import java.time.Instant

sealed class DemoEvent(val at: Instant = Instant.now()) {
    data class TokenReceived(
        val tokenId: Long,
        val beaconId: Long,
        val phoneId: Long,
        val counter: Long,
        val isValid: Boolean
    ) : DemoEvent()

    data class OutboundCreated(
        val messageId: Long,
        val beaconId: Long,
        val opType: String,
        val redundancy: Int
    ) : DemoEvent()

    data class DeliveryClaimed(
        val messageId: Long,
        val phoneId: Long
    ) : DemoEvent()

    data class AckProcessed(
        val messageId: Long,
        val ackStatus: String
    ) : DemoEvent()

    data class InboundReceived(
        val beaconId: Long,
        val msgType: String,
        val opType: String
    ) : DemoEvent()

    data class KeyRotation(
        val beaconId: Long,
        val phase: String // "INIT" | "FINISH"
    ) : DemoEvent()
}