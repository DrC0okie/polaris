package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.PhonePayloadDto
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
    private val outboundMessageRepository: OutboundMessageRepository
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
            serverMsgId = serverMsgId,
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
    fun processAck(request: AckRequestDto) {
        ackProcessor.process(request)
    }
}