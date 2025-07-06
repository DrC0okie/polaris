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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@ApplicationScoped
class PayloadService(
    private val sealer: IMessageSealer,
    private val ackProcessor: PayloadAckProcessor,
    private val beaconRepository: BeaconRepository,
    private val outboundMessageRepository: OutboundMessageRepository,
    private val inboundPayloadProcessor: InboundPayloadProcessor
) {
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
        return message
    }

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

    @OptIn(ExperimentalUnsignedTypes::class)
    @Transactional
    fun processAck(request: AckRequestDto): AckResponseDto {
        return ackProcessor.process(request)
    }
}