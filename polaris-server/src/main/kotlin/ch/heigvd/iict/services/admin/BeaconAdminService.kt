package ch.heigvd.iict.services.admin

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

/**
 * Service providing business logic for beacon management via the admin interface.
 * Encapsulates all CRUD operations for [Beacon] entities.
 */
@ApplicationScoped
class BeaconAdminService {

    @Inject
    private lateinit var beaconRepository: BeaconRepository

    @Inject
    private lateinit var tokenRecordRepository: PoLTokenRecordRepository

    /**
     * Retrieves all registered beacons, sorted by name.
     * @return A list of all [Beacon] entities.
     */
    fun listAllBeacons(): List<Beacon> {
        return beaconRepository.listAll(Sort.ascending("name"))
    }

    /**
     * Finds a single beacon by its database primary key.
     * @param id The database ID of the beacon.
     * @return The found [Beacon] or `null` if it does not exist.
     */
    fun findBeaconById(id: Long): Beacon? {
        return beaconRepository.findById(id)
    }

    /**
     * Creates and persists a new beacon.
     *
     * @param technicalId The unique technical ID of the beacon.
     * @param name Name of the beacon.
     * @param locationDescription A description of the beacon's location.
     * @param publicKeyEd25519 The beacon's 32-byte Ed25519 public key.
     * @param publicKeyX25519 The beacon's optional 32-byte X25519 public key.
     * @return The newly created [Beacon] entity.
     * @throws IllegalArgumentException if the technical ID already exists or keys have an invalid size.
     */
    @Transactional
    fun addBeacon(
        technicalId: Int,
        name: String,
        locationDescription: String,
        publicKeyEd25519: ByteArray,
        publicKeyX25519: ByteArray?
    ): Beacon {
        if (beaconRepository.findByBeaconTechnicalId(technicalId) != null) {
            throw IllegalArgumentException("Beacon with technical ID $technicalId already exists.")
        }
        if (publicKeyEd25519.size != 32) {
            throw IllegalArgumentException("Ed25519 public key must be 32 bytes.")
        }
        if (publicKeyX25519 != null && publicKeyX25519.size != 32) {
            throw IllegalArgumentException("X25519 Public key must be 32 bytes.")
        }

        val beacon = Beacon().apply {
            this.beaconId = technicalId
            this.name = name
            this.locationDescription = locationDescription
            this.publicKey = publicKeyEd25519
            this.publicKeyX25519 = publicKeyX25519
            this.lastKnownCounter = 0L
        }
        beaconRepository.persist(beacon)
        return beacon
    }

    /**
     * Updates the name and location description of an existing beacon.
     * @param id The database ID of the beacon to update.
     * @param name The new name for the beacon.
     * @param locationDescription The new location description.
     * @return The updated [Beacon] entity, or `null` if not found.
     */
    @Transactional
    fun updateBeacon(id: Long, name: String, locationDescription: String): Beacon? {
        val beacon = beaconRepository.findById(id)
        beacon?.let {
            it.name = name
            it.locationDescription = locationDescription
        }
        return beacon
    }

    /**
     * Deletes a beacon from the database.
     * @param id The database ID of the beacon to delete.
     * @return `true` if the beacon was successfully deleted, `false` otherwise.
     */
    @Transactional
    fun deleteBeacon(id: Long): Boolean {
        return beaconRepository.deleteById(id)
    }

    /**
     * Counts the number of PoL tokens submitted for a specific beacon.
     * @param beaconId The database ID of the beacon.
     * @return The total count of associated [PoLTokenRecord]s.
     */
    fun getBeaconTokenCount(beaconId: Long): Long {
        val beacon = beaconRepository.findById(beaconId) ?: return 0
        return tokenRecordRepository.count("beacon", beacon)
    }

    /**
     * Updates the X25519 public key of an existing beacon.
     * This is typically done during a key rotation process.
     *
     * @param id The database ID of the beacon to update.
     * @param publicKeyX25519 The new 32-byte X25519 public key.
     * @return The updated [Beacon] entity, or `null` if not found.
     * @throws IllegalArgumentException if the key has an invalid size.
     */
    @Transactional
    fun updateBeaconX25519Key(id: Long, publicKeyX25519: ByteArray): Beacon? {
        if (publicKeyX25519.size != 32) {
            throw IllegalArgumentException("X25519 Public key must be 32 bytes.")
        }
        val beacon = beaconRepository.findById(id)
        beacon?.let {
            it.publicKeyX25519 = publicKeyX25519
        }
        return beacon
    }
}