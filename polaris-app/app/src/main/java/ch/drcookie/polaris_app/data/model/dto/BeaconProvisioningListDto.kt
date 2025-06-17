package ch.drcookie.polaris_app.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class BeaconProvisioningListDto(
    val beacons: List<BeaconProvisioningDto>
)