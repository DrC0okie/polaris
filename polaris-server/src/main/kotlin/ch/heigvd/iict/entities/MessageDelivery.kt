package ch.heigvd.iict.entities


import ch.heigvd.iict.services.protocol.AckStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "message_deliveries")
@SequenceGenerator(
    name = "message_deliveries_seq",
    sequenceName = "message_deliveries_seq",
    allocationSize = 50
)
class MessageDelivery : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_deliveries_seq")
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_message_id", nullable = false)
    lateinit var outboundMessage: OutboundMessage

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    lateinit var phone: RegisteredPhone

    @Column(nullable = false, updatable = false)
    lateinit var deliveredAt: Instant

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var ackStatus: AckStatus

    var ackReceivedAt: Instant? = null

    @Column(columnDefinition = "bytea")
    var rawAckBlob: ByteArray? = null

    @PrePersist
    fun onPrePersist() {
        deliveredAt = Instant.now()
    }
}