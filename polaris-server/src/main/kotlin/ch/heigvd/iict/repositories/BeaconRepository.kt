package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.Beacon
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BeaconRepository : PanacheRepository<Beacon> {

    fun findByBeaconTechnicalId(id: Int): Beacon? {
        return find("beaconId", id).firstResult()
    }

    fun listAllForProvisioning(): List<Beacon> {
        return listAll()
    }
}