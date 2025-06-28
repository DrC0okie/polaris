package ch.drcookie.polaris_sdk.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Serializer for [kotlinx.serialization].
 *
 * Handles the conversion of [UByteArray] instances to and from Base64-encoded strings
 * within our DTOs and other serializable models.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object UByteArrayBase64Serializer : KSerializer<UByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("UByteArray", PrimitiveKind.STRING)

    @OptIn(ExperimentalEncodingApi::class) // For Base64
    override fun serialize(encoder: Encoder, value: UByteArray) {
        encoder.encodeString(Base64.Default.encode(value.asByteArray()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): UByteArray {
        return Base64.Default.decode(decoder.decodeString()).asUByteArray()
    }
}