package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PoLTokenRecordRepository : PanacheRepository<PoLTokenRecord> {
    
    fun findByBeacon(beacon: Beacon): List<PoLTokenRecord> {
        return list("beacon", beacon)
    }

    fun findByPhone(phone: RegisteredPhone): List<PoLTokenRecord> {
        return list("phone", phone)
    }

    fun exists(beacon: Beacon, beaconCounter: Long, nonceHex: String): Boolean {
        return count(
            "beacon = :beacon and beaconCounter = :counter and nonceHex = :nonceHex",
            Parameters.with("beacon", beacon)
                .and("counter", beaconCounter)
                .and("nonceHex", nonceHex)
        ) > 0
    }
}