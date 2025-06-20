package ch.drcookie.polaris_app.data.model.dto

import ch.drcookie.polaris_app.util.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class PhoneRegistrationRequestDto(
    @Serializable(with = UByteArrayBase64Serializer::class)
    val publicKey: UByteArray,
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhoneRegistrationRequestDto

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (deviceModel != other.deviceModel) return false
        if (osVersion != other.osVersion) return false
        if (appVersion != other.appVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + (deviceModel?.hashCode() ?: 0)
        result = 31 * result + (osVersion?.hashCode() ?: 0)
        result = 31 * result + (appVersion?.hashCode() ?: 0)
        return result
    }

}