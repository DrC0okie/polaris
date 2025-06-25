package ch.drcookie.polaris_sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
internal data class BeaconProvisioningListDto(
    internal val beacons: List<BeaconProvisioningDto>
)