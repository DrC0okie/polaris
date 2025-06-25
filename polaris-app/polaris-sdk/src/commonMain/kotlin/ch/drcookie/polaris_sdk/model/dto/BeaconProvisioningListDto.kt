package ch.drcookie.polaris_sdk.model.dto

import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class BeaconProvisioningListDto(
    val beacons: List<BeaconProvisioningDto>
)