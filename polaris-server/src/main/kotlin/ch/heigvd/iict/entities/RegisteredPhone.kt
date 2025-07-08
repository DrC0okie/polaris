package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

/**
 * Represents a mobile device that has registered with the server.
 *
 * This record holds the device's identity (public key) and credentials (API key)
 * needed to interact with the secure API endpoints.
 */
@Entity
@Table(name = "registered_phones", indexes = [
    Index(name = "idx_phone_api_key", columnList = "apiKey", unique = true)
])
@SequenceGenerator(
    name = "registered_phones_seq",
    sequenceName = "registered_phones_seq",
    allocationSize = 50
)
class RegisteredPhone : PanacheEntityBase {

    /** The primary key for this phone in the database. This is the `phoneId`. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registered_phones_seq")
    var id: Long? = null

    /** The device's unique and immutable Ed25519 public key. */
    @Column(name = "public_key", columnDefinition = "BYTEA", nullable = false, unique = true)
    lateinit var publicKey: ByteArray

    /** The unique API key assigned to this device for authenticating requests. */
    @Column(name = "api_key", unique = true, nullable = false, length = 70)
    lateinit var apiKey: String

    /** A string containing device metadata (model, OS, app version) for diagnostics. */
    var userAgent: String? = null

    /** The timestamp of the last time this phone interacted with the server. */
    var lastSeenAt: Instant? = null

    /** The timestamp when this phone record was first created. */
    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    /** The timestamp when this phone record was last updated. */
    @Column(nullable = false)
    lateinit var updatedAt: Instant

    /** Sets the [createdAt] and [updatedAt] timestamps on initial persistence. */
    @PrePersist
    fun onPrePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    /** Updates the [updatedAt] timestamp whenever the entity is modified. */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}