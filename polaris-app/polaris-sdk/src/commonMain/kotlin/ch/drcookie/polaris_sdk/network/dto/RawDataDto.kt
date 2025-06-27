package ch.drcookie.polaris_sdk.network.dto

import ch.drcookie.polaris_sdk.util.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

/**
 * A generic DTO for transferring a raw, B64-encoded data blob.
 * Can be used for requests where only a single data payload is needed.
 */
@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
internal data class RawDataDto(
    @Serializable(with = UByteArrayBase64Serializer::class)
    val data: UByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RawDataDto

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
