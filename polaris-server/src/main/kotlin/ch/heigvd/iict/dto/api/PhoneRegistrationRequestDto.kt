package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class PhoneRegistrationRequestDto(
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val publicKey: UByteArray,

    val deviceModel: String?, // Ex: "Pixel 7 Pro"
    val osVersion: String?,   // Ex: "Android 14"
    val appVersion: String?   // Ex: "PolarisApp 1.0.2"
)