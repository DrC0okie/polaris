package ch.drcookie.polaris_app

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Custom serializer for UByteArray to Base64 String
@OptIn(ExperimentalUnsignedTypes::class)
object UByteArrayBase64Serializer : KSerializer<UByteArray> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "UByteArray",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    @OptIn(ExperimentalEncodingApi::class) // For Base64
    override fun serialize(encoder: Encoder, value: UByteArray) {
        encoder.encodeString(Base64.Default.encode(value.asByteArray()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): UByteArray {
        return Base64.Default.decode(decoder.decodeString()).asUByteArray()
    }
}