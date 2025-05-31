package ch.heigvd.iict.services.api

import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.dto.api.BeaconProvisioningDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class ProvisioningApiService {

    @Inject
    private lateinit var beaconRepository: BeaconRepository

    fun getBeaconsForProvisioning(): List<BeaconProvisioningDto> {
        return beaconRepository.listAllForProvisioning().map { beacon ->
            BeaconProvisioningDto(
                beaconId = beacon.beaconId.toUInt(),
                name = beacon.name,
                locationDescription = beacon.locationDescription,
                publicKey = beacon.publicKey,
                lastKnownCounter = beacon.lastKnownCounter.toULong()
            )
        }
    }
}