package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.Instant

/**
 * Represents a PoL token (valid or not).
 *
 * This table serves as the authoritative log of all PoL claims submitted to the server.
 */
@Entity
@Table(name = "pol_token_records", indexes = [
    Index(name = "idx_poltoken_unique_proof", columnList = "beacon_id, beaconCounter, nonce_hex", unique = true)
])
@SequenceGenerator(
    name = "pol_token_records_seq",
    sequenceName = "pol_token_records_seq",
    allocationSize = 50
)
class PoLTokenRecord : PanacheEntityBase {

    /** The primary key for this token record. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pol_token_records_seq")
    var id: Long? = null

    /** The flags field from the original PoL transaction. */
    var flags: Byte = 0

    /** A reference to the [RegisteredPhone] that submitted this token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id_fk", nullable = false)
    lateinit var phone: RegisteredPhone

    /** A reference to the [Beacon] that issued this token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id", nullable = false)
    lateinit var beacon: Beacon

    /** The beacon's monotonic counter value at the time of the transaction. */
    var beaconCounter: Long = 0L

    /** The hexadecimal representation of the nonce used in the transaction, for uniqueness checks. */
    @Column(name = "nonce_hex", length = 32, nullable = false)
    lateinit var nonceHex: String

    /** The public key of the phone used in this specific transaction. */
    @Column(name = "phone_pk_used", columnDefinition = "BYTEA", nullable = false)
    lateinit var phonePkUsed: ByteArray

    /** The public key of the beacon used in this specific transaction. */
    @Column(name = "beacon_pk_used", columnDefinition = "BYTEA", nullable = false)
    lateinit var beaconPkUsed: ByteArray

    /** The phone's signature from the PoL token. */
    @Column(name = "phone_sig", columnDefinition = "BYTEA", nullable = false)
    lateinit var phoneSig: ByteArray

    /** The beacon's signature from the PoL token. */
    @Column(name = "beacon_sig", columnDefinition = "BYTEA", nullable = false)
    lateinit var beaconSig: ByteArray

    /** A boolean flag indicating whether the token passed all validation checks. */
    var isValid: Boolean = false
    @Column(length = 255)
    var validationError: String? = null

    /** The timestamp when the server received this token. */
    @Column(nullable = false, updatable = false)
    lateinit var receivedAt: Instant

    /** Sets the [receivedAt] timestamp on initial persistence. */
    @PrePersist
    fun onPrePersist() {
        receivedAt = Instant.now()
    }
}
