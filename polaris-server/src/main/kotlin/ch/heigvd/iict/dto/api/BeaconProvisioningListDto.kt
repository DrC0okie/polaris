package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class BeaconProvisioningListDto(
    val beacons: List<BeaconProvisioningDto>
)