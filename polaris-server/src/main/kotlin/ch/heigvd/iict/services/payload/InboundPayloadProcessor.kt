package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.BeaconPayloadDto
import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.InboundMessage
import ch.heigvd.iict.repositories.InboundMessageRepository // Create this new repo
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class InboundPayloadProcessor(
    private val unsealer: IMessageUnsealer,
    private val inboundMessageRepository: InboundMessageRepository
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun process(request: BeaconPayloadDto, sourceBeacon: Beacon): PlaintextMessage {
        // Unpack and decrypt the blob
        val sealedMessage = SealedMessage.fromBlob(request.data.asByteArray())
        val plaintext = unsealer.unseal(sealedMessage, sourceBeacon)

        // Idempotency check: Has this message already been processed?
        if (inboundMessageRepository.existsByBeaconAndMsgId(sourceBeacon, plaintext.msgId)) {
            throw IllegalStateException("Duplicate message received from beacon ${sourceBeacon.id} with msgId ${request.beaconId}")
        }

        // Validate the plaintext and update beacon state
        if (plaintext.beaconCounter > sourceBeacon.lastKnownCounter) {
            sourceBeacon.lastKnownCounter = plaintext.beaconCounter
        }

        // Create a record of the inbound message
        val inboundRecord = InboundMessage().apply {
            beacon = sourceBeacon
            beaconMsgId = plaintext.msgId
            msgType = plaintext.msgType
            opType = plaintext.opType
            beaconCounter = plaintext.beaconCounter
            // Convert payload bytes to a UTF-8 String to store as JSON
            payload = plaintext.payload.toString(Charsets.UTF_8)
        }
        inboundRecord.persist()
        Log.info("Stored inbound message ${inboundRecord.id} from beacon ${sourceBeacon.id}")

        return plaintext
    }
}