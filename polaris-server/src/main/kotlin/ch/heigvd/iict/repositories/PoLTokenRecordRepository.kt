package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped

/**
 * Panache repository for managing [PoLTokenRecord] entities.
 */
@ApplicationScoped
class PoLTokenRecordRepository : PanacheRepository<PoLTokenRecord> {

    /**
     * Finds all token records associated with a specific beacon.
     * @param beacon The [Beacon] entity to filter by.
     * @return A list of [PoLTokenRecord]s.
     */
    fun findByBeacon(beacon: Beacon): List<PoLTokenRecord> {
        return list("beacon", beacon)
    }

    /**
     * Finds all token records submitted by a specific phone.
     * @param phone The [RegisteredPhone] entity to filter by.
     * @return A list of [PoLTokenRecord]s.
     */
    fun findByPhone(phone: RegisteredPhone): List<PoLTokenRecord> {
        return list("phone", phone)
    }

    /**
     * Checks for the existence of a token with a specific proof triple (beacon, counter, nonce).
     *
     * @param beacon The [Beacon] associated with the proof.
     * @param beaconCounter The counter value from the proof.
     * @param nonceHex The nonce from the proof, as a hexadecimal string.
     * @return `true` if a record with this proof already exists, `false` otherwise.
     */
    fun exists(beacon: Beacon, beaconCounter: Long, nonceHex: String): Boolean {
        return count(
            "beacon = :beacon and beaconCounter = :counter and nonceHex = :nonceHex",
            Parameters.with("beacon", beacon)
                .and("counter", beaconCounter)
                .and("nonceHex", nonceHex)
        ) > 0
    }
}