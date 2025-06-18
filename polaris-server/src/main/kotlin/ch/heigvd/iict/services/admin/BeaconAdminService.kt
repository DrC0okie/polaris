package ch.heigvd.iict.services.admin

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

@ApplicationScoped
class BeaconAdminService {

    @Inject
    private lateinit var beaconRepository: BeaconRepository

    @Inject
    private lateinit var tokenRecordRepository: PoLTokenRecordRepository

    fun listAllBeacons(): List<Beacon> {
        return beaconRepository.listAll(Sort.ascending("name"))
    }

    fun findBeaconById(id: Long): Beacon? {
        return beaconRepository.findById(id)
    }

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

    @Transactional
    fun updateBeacon(id: Long, name: String, locationDescription: String): Beacon? {
        val beacon = beaconRepository.findById(id)
        beacon?.let {
            it.name = name
            it.locationDescription = locationDescription
        }
        return beacon
    }

    @Transactional
    fun deleteBeacon(id: Long): Boolean {
        return beaconRepository.deleteById(id)
    }

    fun getBeaconTokenCount(beaconId: Long): Long {
        val beacon = beaconRepository.findById(beaconId) ?: return 0
        return tokenRecordRepository.count("beacon", beacon)
    }

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