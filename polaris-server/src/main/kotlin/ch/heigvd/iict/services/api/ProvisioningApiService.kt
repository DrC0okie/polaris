package ch.heigvd.iict.services.api

import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.dto.api.BeaconProvisioningDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Service responsible for providing beacon provisioning data to mobile clients.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class ProvisioningApiService {

    @Inject
    private lateinit var beaconRepository: BeaconRepository

    /**
     * Retrieves all active beacons and maps them to the [BeaconProvisioningDto] format.
     * This data is sent to mobile clients to inform them of the beacons they can interact with.
     *
     * @return A list of [BeaconProvisioningDto]s.
     */
    fun getBeaconsForProvisioning(): List<BeaconProvisioningDto> {
        return beaconRepository.listAllForProvisioning().map {
            BeaconProvisioningDto(
                it.beaconId.toUInt(),
                it.name,
                it.locationDescription,
                it.publicKey.asUByteArray(),
                it.lastKnownCounter.toULong()
            )
        }
    }
}