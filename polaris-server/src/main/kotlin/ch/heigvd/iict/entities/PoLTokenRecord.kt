package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "pol_token_records", indexes = [
    // Assure l'unicité d'une preuve (balise, compteur de la balise, nonce)
    Index(name = "idx_poltoken_unique_proof", columnList = "beacon_id, beaconCounter, nonce_hex", unique = true)
])
class PoLTokenRecord : PanacheEntity() {

    var flags: Byte = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id_fk", nullable = false)
    lateinit var phone: RegisteredPhone

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id", nullable = false)
    lateinit var beacon: Beacon

    var beaconCounter: Long = 0L

    @Column(name = "nonce_hex", length = 32, nullable = false)
    lateinit var nonceHex: String

    @Column(name = "phone_pk_used", columnDefinition = "BYTEA", nullable = false)
    lateinit var phonePkUsed: ByteArray

    @Column(name = "beacon_pk_used", columnDefinition = "BYTEA", nullable = false)
    lateinit var beaconPkUsed: ByteArray

    @Column(name = "phone_sig", columnDefinition = "BYTEA", nullable = false)
    lateinit var phoneSig: ByteArray

    @Column(name = "beacon_sig", columnDefinition = "BYTEA", nullable = false)
    lateinit var beaconSig: ByteArray

    var isValid: Boolean = false
    @Column(length = 255)
    var validationError: String? = null

    @Column(nullable = false, updatable = false)
    lateinit var receivedAt: Instant

    @PrePersist
    fun onPrePersist() {
        receivedAt = Instant.now()
    }
}
