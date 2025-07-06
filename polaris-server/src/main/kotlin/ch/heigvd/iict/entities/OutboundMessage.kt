package ch.heigvd.iict.entities

import ch.heigvd.iict.services.protocol.MessageStatus
import ch.heigvd.iict.services.protocol.OperationType
import jakarta.persistence.*
import java.time.Instant
import io.hypersistence.utils.hibernate.type.json.JsonType
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import org.hibernate.annotations.Type

@Entity
@Table(name = "outbound_messages")
@SequenceGenerator(
    name = "outbound_messages_seq",
    sequenceName = "outbound_messages_seq",
    allocationSize = 50
)
class OutboundMessage : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbound_messages_seq")
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id", nullable = false)
    lateinit var beacon: Beacon

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var status: MessageStatus

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb", nullable = false)
    lateinit var commandPayload: String // Store JSON as a string

    @Column(columnDefinition = "bytea")
    var encryptedBlob: ByteArray? = null

    @Column(nullable = false, unique = true)
    var serverMsgId: Long = 0L

    @Column(nullable = false)
    var redundancyFactor: Int = 1

    @Column(nullable = false)
    var deliveryCount: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var opType: OperationType

    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    var firstAcknowledgedAt: Instant? = null

    @OneToMany(mappedBy = "outboundMessage", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    lateinit var deliveries: MutableList<MessageDelivery>

    @PrePersist
    fun onPrePersist() {
        createdAt = Instant.now()
    }
}