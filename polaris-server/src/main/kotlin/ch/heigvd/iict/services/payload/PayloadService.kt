package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.AckResponseDto
import ch.heigvd.iict.dto.api.BeaconPayloadDto
import ch.heigvd.iict.dto.api.PhonePayloadDto
import ch.heigvd.iict.dto.api.RawDataDto
import ch.heigvd.iict.entities.*
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.services.protocol.AckStatus
import ch.heigvd.iict.services.protocol.MessageStatus
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.repositories.OutboundMessageRepository
import ch.heigvd.iict.services.protocol.MessageType
import ch.heigvd.iict.services.protocol.OperationType
import ch.heigvd.iict.web.demo.DemoEvent
import ch.heigvd.iict.web.demo.DemoSseResource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * High-level service for managing the lifecycle of secure payloads.
 *
 * This service acts as a facade for the entire data mule functionality, managing
 * the creation, delivery, and acknowledgment of messages between the server and beacons.
 */
@ApplicationScoped
class PayloadService(
    private val sealer: IMessageSealer,
    private val ackProcessor: PayloadAckProcessor,
    private val beaconRepository: BeaconRepository,
    private val outboundMessageRepository: OutboundMessageRepository,
    private val inboundPayloadProcessor: InboundPayloadProcessor,
    private val demoSse: DemoSseResource // For demo
) {

    /**
     * Creates, encrypts, and persists a new outbound message job.
     *
     * @param beaconId The technical ID of the target beacon.
     * @param command The JSON payload for the command.
     * @param opType The [OperationType] of the command.
     * @param redundancyFactor The number of phones that should attempt delivery.
     * @return The created [OutboundMessage] entity.
     * @throws jakarta.ws.rs.NotFoundException if the beacon ID is invalid.
     * @throws IllegalStateException if the target beacon is not provisioned for encrypted communication.
     */
    @Transactional
    fun createOutboundMessage(
        beaconId: Int,
        command: JsonObject,
        opType: OperationType,
        redundancyFactor: Int = 1
    ): OutboundMessage {
        val beacon = beaconRepository.findByBeaconTechnicalId(beaconId)
            ?: throw NotFoundException("Beacon with technical ID $beaconId not found.")

        if (beacon.publicKeyX25519 == null) {
            throw IllegalStateException("Cannot create encrypted message: Beacon $beaconId has no X25519 public key.")
        }

        val serverMsgId = outboundMessageRepository.getNextServerMsgId()

        // Create the plaintext message with the correct structure
        val plaintext = PlaintextMessage(
            msgId = serverMsgId,
            msgType = MessageType.REQ,
            opType = opType,
            beaconCounter = beacon.lastKnownCounter,
            payload = Json.encodeToString(command).toByteArray()
        )

        // Use the sealer to encrypt the message. The business logic doesn't know *how* it's sealed.
        val sealedMessage = sealer.seal(plaintext, beacon)

        // Create and persist the entity
        val message = OutboundMessage().apply {
            this.beacon = beacon
            this.status = MessageStatus.PENDING
            this.commandPayload = Json.encodeToString(command)
            this.encryptedBlob = sealedMessage.toBlob()
            this.serverMsgId = serverMsgId
            this.redundancyFactor = redundancyFactor
            this.opType = opType
        }
        message.persist()

        // For demo
        if (opType == OperationType.ROTATE_KEY_INIT) {
            demoSse.publish(
                DemoEvent.KeyRotation(
                    beaconId = beacon.id!!,
                    phase = "INIT"
                )
            )
        } else {
            demoSse.publish(
                DemoEvent.OutboundCreated(
                    messageId = message.id!!,
                    beaconId = message.beacon.id!!,
                    opType = message.opType.name,
                    redundancy = message.redundancyFactor
                )
            )
        }

        return message
    }

    /**
     * Retrieves and claims a list of pending outbound message jobs for a given phone.
     *
     * This method is concurrency-safe and ensures that a single job is not delivered
     * more times than specified by its `redundancyFactor`.
     *
     * @param phone The [RegisteredPhone] requesting work.
     * @param maxJobs The maximum number of jobs to return.
     * @return A list of [PhonePayloadDto]s, each representing a job for the phone to deliver.
     */
    @Transactional
    @OptIn(ExperimentalUnsignedTypes::class)
    fun getPendingPayloadsForPhone(phone: RegisteredPhone, maxJobs: Int = 1): List<PhonePayloadDto> {
        // Ask the repository to find and lock a list of jobs
        val claimedMessages = outboundMessageRepository.findAndClaimAvailableJob(phone, maxJobs)
        if (claimedMessages.isEmpty()) return emptyList()

        // Process each successfully claimed message.
        return claimedMessages.map { claimedMessage ->

            val delivery = MessageDelivery().apply {
                this.outboundMessage = claimedMessage
                this.phone = phone
                this.ackStatus = AckStatus.PENDING_ACK
            }
            delivery.persist()

            demoSse.publish(
                DemoEvent.DeliveryClaimed(
                    messageId = delivery.outboundMessage.id!!,
                    phoneId = delivery.phone.id!!
                )
            )

            // Update the parent message's state.
            claimedMessage.deliveryCount++
            if (claimedMessage.status == MessageStatus.PENDING) {
                claimedMessage.status = MessageStatus.DELIVERING
            }

            // Create the DTO to be sent back to the phone.
            PhonePayloadDto(
                deliveryId = delivery.id!!,
                beaconId = claimedMessage.beacon.beaconId,
                encryptedBlob = claimedMessage.encryptedBlob!!.asUByteArray()
            )
        }
    }

    /**
     * Processes an inbound payload that was initiated by a beacon.
     *
     * This method handles a message sent from a beacon to the server (e.g., a status update).
     * It decrypts the message, stores it, and crafts a sealed ACK to be sent back to the beacon.
     *
     * @param request The DTO containing the raw encrypted blob from the beacon.
     * @return A [RawDataDto] containing the sealed ACK for the phone to relay back to the beacon.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @Transactional
    fun processInboundPayload(request: BeaconPayloadDto): RawDataDto {
        val beacon = beaconRepository.findByBeaconTechnicalId(request.beaconId.toInt())
            ?: throw NotFoundException("Beacon with technical ID ${request.beaconId} not found.")

        if (beacon.publicKeyX25519 == null) {
            // Can't process if we can't establish a secure channel
            throw IllegalStateException("Beacon ${request.beaconId} is not provisioned for encrypted communication.")
        }

        // Delegate processing and storage to the dedicated processor
        val receivedPlaintext = inboundPayloadProcessor.process(request, beacon) // Inject inboundPayloadProcessor

        // Craft the ACK response to send back
        val ackPlaintext = PlaintextMessage(
            msgId = receivedPlaintext.msgId, // Echo the message ID in the ack
            msgType = MessageType.ACK,
            opType = receivedPlaintext.opType, // Echo back the opType from the beacon's request
            beaconCounter = 0L, // The server's counter is not used in this context
            payload = ByteArray(0) // ACK has no payload
        )

        // Seal the ACK
        val sealedAck = sealer.seal(ackPlaintext, beacon)

        // Wrap it in the DTO for the phone
        return RawDataDto(data = sealedAck.toBlob().asUByteArray())
    }

    /**
     * Processes an acknowledgment for a server-to-beacon message delivery.
     * This method simply delegates the call to the [PayloadAckProcessor].
     *
     * @param request The DTO containing the delivery ID and the ACK blob.
     * @return An [AckResponseDto] with the processing result.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @Transactional
    fun processAck(request: AckRequestDto): AckResponseDto {
        return ackProcessor.process(request)
    }
}