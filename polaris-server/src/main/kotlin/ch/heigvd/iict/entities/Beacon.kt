package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "beacons")
@SequenceGenerator(
    name = "beacons_seq",
    sequenceName = "beacons_seq",
    allocationSize = 50
)
class Beacon : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "beacons_seq")
    var id: Long? = null

    @Column(unique = true, nullable = false)
    var beaconId: Int = 0

    @Column(nullable = false, columnDefinition = "BYTEA")
    lateinit var publicKey: ByteArray

    @Column(name = "public_key_x25519", columnDefinition = "BYTEA", nullable = true, unique = true)
    var publicKeyX25519: ByteArray? = null

    @Column(nullable = false)
    var lastKnownCounter: Long = 0L

    lateinit var name: String
    lateinit var locationDescription: String

    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = false)
    lateinit var updatedAt: Instant

    @PrePersist
    fun onPrePersist() {
        val now = Instant.now()
        if (!::createdAt.isInitialized) {
            createdAt = now
        }
        updatedAt = now
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
