package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.Beacon
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

/**
 * Panache repository for managing [Beacon] entities.
 */
@ApplicationScoped
class BeaconRepository : PanacheRepository<Beacon> {

    /**
     * Finds a [Beacon] by its unique technical ID (hardcoded in firmware).
     * @param id The technical ID of the beacon.
     * @return The found [Beacon] or `null` if not found.
     */
    fun findByBeaconTechnicalId(id: Int): Beacon? {
        return find("beaconId", id).firstResult()
    }

    /**
     * Retrieves all beacons to be included in the provisioning data for mobile clients.
     * @return A list of all registered [Beacon] entities.
     */
    fun listAllForProvisioning(): List<Beacon> {
        return listAll()
    }
}