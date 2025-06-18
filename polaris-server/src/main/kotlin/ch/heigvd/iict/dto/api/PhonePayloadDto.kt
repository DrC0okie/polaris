package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class PhonePayloadDto(
    // This is the ID of the specific delivery record, NOT the outbound message ID.
    // The phone must echo this back when submitting the ACK.
    val deliveryId: Long,

    // The beacon technical ID, so the phone knows which beacon to connect to.
    val beaconId: Int,

    // The fully serialized and encrypted payload blob to be sent over BLE.
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val encryptedBlob: UByteArray
)
