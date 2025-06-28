package ch.heigvd.iict.entities

import ch.heigvd.iict.services.protocol.MessageType
import ch.heigvd.iict.services.protocol.OperationType
import io.hypersistence.utils.hibernate.type.json.JsonType
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant

@Entity
@Table(name = "inbound_messages")
@SequenceGenerator(name = "inbound_messages_seq", sequenceName = "inbound_messages_seq", allocationSize = 50)
class InboundMessage : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inbound_messages_seq")
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id", nullable = false)
    lateinit var beacon: Beacon

    @Column(nullable = false)
    var beaconMsgId: Long = 0L

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var msgType: MessageType

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var opType: OperationType

    @Column(nullable = false)
    var beaconCounter: Long = 0L

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    var payload: String? = null // The status JSON from the beacon

    @Column(nullable = false, updatable = false)
    lateinit var receivedAt: Instant

    @PrePersist
    fun onPrePersist() {
        receivedAt = Instant.now()
    }
}