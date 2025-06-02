package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class PoLTokenDto(
    val flags: UByte,
    val phoneId: ULong, // 8 bytes
    val beaconId: UInt, // 4 bytes
    val beaconCounter: ULong,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val nonce: UByteArray,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val phonePk: UByteArray,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val beaconPk: UByteArray,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val phoneSig: UByteArray,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val beaconSig: UByteArray
)