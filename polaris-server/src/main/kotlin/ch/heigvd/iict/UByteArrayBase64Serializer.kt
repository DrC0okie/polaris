package ch.heigvd.iict

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Base64

@OptIn(ExperimentalUnsignedTypes::class)
object UByteArrayBase64Serializer : KSerializer<UByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UByteArray) {
        // Server typically doesn't serialize this specific DTO back *to* the client in this way,
        // but good to have for completeness or if you do.
        encoder.encodeString(Base64.getEncoder().encodeToString(value.asByteArray()))
    }

    override fun deserialize(decoder: Decoder): UByteArray {
        return Base64.getDecoder().decode(decoder.decodeString()).asUByteArray()
    }
}