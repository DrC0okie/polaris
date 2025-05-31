package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class PoLTokenDto(
    val flags: UByte,
    val phoneId: ULong,
    val beaconId: UInt,
    val beaconCounter: ULong,
    val nonce: ByteArray,
    val phonePk: ByteArray,
    val beaconPk: ByteArray,
    val phoneSig: ByteArray,
    val beaconSig: ByteArray
)