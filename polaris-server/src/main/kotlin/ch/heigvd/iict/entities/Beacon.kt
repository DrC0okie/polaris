package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

/**
 * Represents a physical Polaris beacon.
 *
 * This table stores all the essential, static information about a beacon, including its
 * cryptographic keys and its last known state.
 */
@Entity
@Table(name = "beacons")
@SequenceGenerator(
    name = "beacons_seq",
    sequenceName = "beacons_seq",
    allocationSize = 50
)
class Beacon : PanacheEntityBase {

    /** The primary key for this beacon in the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "beacons_seq")
    var id: Long? = null

    /**
     * The unique technical identifier for the beacon, hardcoded in its firmware.
     * This ID is used for discovery in BLE advertisements.
     */
    @Column(unique = true, nullable = false)
    var beaconId: Int = 0

    /** The beacon's Ed25519 public key, used for signing PoL tokens and broadcasts. */
    @Column(nullable = false, columnDefinition = "BYTEA")
    lateinit var publicKey: ByteArray

    /**
     * The beacon's X25519 public key, used for the Diffie-Hellman key exchange to establish
     * a secure, end-to-end encrypted channel with the server.
     */
    @Column(name = "public_key_x25519", columnDefinition = "BYTEA", nullable = true, unique = true)
    var publicKeyX25519: ByteArray? = null

    /**
     * The most recent monotonic counter value received from this beacon.
     * This is used to prevent replay attacks on PoL tokens.
     */
    @Column(nullable = false)
    var lastKnownCounter: Long = 0L

    /** A human-readable name for the beacon. */
    lateinit var name: String

    /** A description of the beacon's physical placement. */
    lateinit var locationDescription: String

    /** The timestamp when this beacon record was first created. */
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    /** The timestamp when this beacon record was last updated. */
    @Column(nullable = false)
    lateinit var updatedAt: Instant

    /** Sets the [createdAt] and [updatedAt] timestamps on initial persistence. */
    @PrePersist
    fun onPrePersist() {
        val now = Instant.now()
        if (!::createdAt.isInitialized) {
            createdAt = now
        }
        updatedAt = now
    }

    /** Updates the [updatedAt] timestamp whenever the entity is modified. */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
