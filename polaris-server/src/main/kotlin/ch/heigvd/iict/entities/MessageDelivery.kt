package ch.heigvd.iict.entities

import ch.heigvd.iict.services.protocol.AckStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

/**
 * Tracks the delivery of a single [OutboundMessage] to a specific [RegisteredPhone].
 *
 * This record is created when a phone fetches an outbound message job. It is later updated when
 * the phone submits the corresponding acknowledgment from the beacon.
 */
@Entity
@Table(name = "message_deliveries")
@SequenceGenerator(
    name = "message_deliveries_seq",
    sequenceName = "message_deliveries_seq",
    allocationSize = 50
)
class MessageDelivery : PanacheEntityBase {

    /** The primary key for this delivery record. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_deliveries_seq")
    var id: Long? = null

    /** A reference to the parent [OutboundMessage] that was delivered. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_message_id", nullable = false)
    lateinit var outboundMessage: OutboundMessage

    /** A reference to the [RegisteredPhone] that acted as the data mule for this delivery. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    lateinit var phone: RegisteredPhone

    /** The timestamp when the phone fetched this message job from the server. */
    @Column(nullable = false, updatable = false)
    lateinit var deliveredAt: Instant

    /** The final status of the acknowledgment processing for this delivery. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var ackStatus: AckStatus

    /** The timestamp when the server received the acknowledgment blob from the phone. */
    var ackReceivedAt: Instant? = null

    /** The raw, encrypted acknowledgment blob received from the beacon. Stored for auditing. */
    @Column(columnDefinition = "bytea")
    var rawAckBlob: ByteArray? = null

    /** Sets the [deliveredAt] timestamp when this record is first created. */
    @PrePersist
    fun onPrePersist() {
        deliveredAt = Instant.now()
    }
}