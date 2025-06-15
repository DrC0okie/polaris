package ch.drcookie.polaris_app.dto

import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class BeaconProvisioningListDto(
    val beacons: List<BeaconProvisioningDto>
)