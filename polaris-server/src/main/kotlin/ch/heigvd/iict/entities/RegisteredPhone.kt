package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "registered_phones", indexes = [
    Index(name = "idx_phone_technical_id", columnList = "phoneTechnicalId", unique = true)
])
class RegisteredPhone : PanacheEntity() {

    @Column(unique = true, nullable = false)
    var phoneTechnicalId: Long = 0L

    @Column(name = "public_key", columnDefinition = "BYTEA", nullable = false, unique = true)
    lateinit var publicKey: ByteArray

    var userAgent: String? = null
    var lastSeenAt: Instant? = null

    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = false)
    lateinit var updatedAt: Instant

    @PrePersist
    fun onPrePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}