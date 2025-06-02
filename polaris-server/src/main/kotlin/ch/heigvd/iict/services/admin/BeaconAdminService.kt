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
    fun addBeacon(technicalId: Int, name: String, locationDescription: String, publicKey: ByteArray): Beacon {
        if (beaconRepository.findByBeaconTechnicalId(technicalId) != null) {
            throw IllegalArgumentException("Beacon with technical ID $technicalId already exists.")
        }
        if (publicKey.size != 32) {
            throw IllegalArgumentException("Public key must be 32 bytes.")
        }

        val beacon = Beacon().apply {
            this.beaconId = technicalId
            this.name = name
            this.locationDescription = locationDescription
            this.publicKey = publicKey
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

    // TODO: Méthodes pour les statistiques (nombre de tokens, etc.)
    fun getBeaconTokenCount(beaconId: Long): Long {
        val beacon = beaconRepository.findById(beaconId) ?: return 0
        return tokenRecordRepository.count("beacon", beacon)
    }
}