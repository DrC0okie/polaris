package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class BeaconProvisioningDto(
    val beaconId: UInt,
    val name: String,
    val locationDescription: String,
    val publicKey: ByteArray,
    val lastKnownCounter: ULong
)