package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class BeaconProvisioningDto(
    val beaconId: UInt,
    val name: String,
    val locationDescription: String,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val publicKey: UByteArray,
    val lastKnownCounter: ULong
)