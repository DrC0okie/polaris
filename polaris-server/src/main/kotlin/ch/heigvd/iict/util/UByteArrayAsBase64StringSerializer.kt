package ch.heigvd.iict.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Base64

@OptIn(ExperimentalUnsignedTypes::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
object UByteArrayAsBase64StringSerializer : KSerializer<UByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("UByteArrayAsBase64String", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UByteArray) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value.asByteArray()))
    }

    override fun deserialize(decoder: Decoder): UByteArray {
        return Base64.getDecoder().decode(decoder.decodeString()).asUByteArray()
    }
}