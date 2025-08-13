package ch.heigvd.iict.web.demo

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class DemoEvent {
    @kotlinx.serialization.Transient
    val at: Instant = Instant.now()

    @Serializable
    data class TokenReceived(
        val tokenId: Long,
        val beaconId: Long,
        val beaconName: String,
        val phoneId: Long,
        val counter: Long,
        val isValid: Boolean
    ) : DemoEvent()

    @Serializable
    data class OutboundCreated(
        val messageId: Long,
        val beaconId: Long,
        val opType: String,
        val redundancy: Int
    ) : DemoEvent()

    @Serializable
    data class DeliveryClaimed(
        val messageId: Long,
        val phoneId: Long
    ) : DemoEvent()

    @Serializable
    data class AckProcessed(
        val messageId: Long,
        val ackStatus: String
    ) : DemoEvent()

    @Serializable
    data class InboundReceived(
        val beaconId: Long,
        val msgType: String,
        val opType: String
    ) : DemoEvent()

    @Serializable
    data class KeyRotation(
        val beaconId: Long,
        val messageId: Long,
        val phase: String // "INIT" | "FINISH"
    ) : DemoEvent()
}