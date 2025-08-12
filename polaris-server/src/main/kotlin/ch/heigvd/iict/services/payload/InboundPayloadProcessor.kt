package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.BeaconPayloadDto
import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.InboundMessage
import ch.heigvd.iict.repositories.InboundMessageRepository
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.web.demo.DemoSseResource
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Service responsible for the core logic of processing a decrypted inbound payload.
 *
 * This class is called after a message from a beacon has been successfully decrypted.
 * It handles idempotency checks, state updates, and persistence.
 *
 * @param unsealer The service used to decrypt the message.
 * @param inboundMessageRepository The repository for storing the inbound message record.
 */
@ApplicationScoped
class InboundPayloadProcessor(
    private val unsealer: IMessageUnsealer,
    private val inboundMessageRepository: InboundMessageRepository,
    private val demoSse: DemoSseResource // For demo
) {

    /**
     * Processes a raw, encrypted payload from a beacon.
     *
     * This method:
     * 1. Decrypts the message.
     * 2. Performs an idempotency check to prevent duplicate processing.
     * 3. Updates the beacon's `lastKnownCounter` if the message is fresh.
     * 4. Persists a record of the inbound message.
     *
     * @param request The DTO containing the raw encrypted blob.
     * @param sourceBeacon The [Beacon] that sent the message.
     * @return The decrypted [PlaintextMessage].
     * @throws IllegalStateException if a duplicate message is detected.
     */
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

        demoSse.publish(
            ch.heigvd.iict.web.demo.DemoEvent.InboundReceived(
                beaconId = inboundRecord.beacon.id!!,
                msgType = inboundRecord.msgType.name,
                opType = inboundRecord.opType.name
            )
        )

        return plaintext
    }
}