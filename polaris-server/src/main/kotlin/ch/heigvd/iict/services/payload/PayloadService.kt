package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.PhonePayloadDto
import ch.heigvd.iict.entities.*
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.MessageDeliveryRepository
import ch.heigvd.iict.services.core.AckStatus
import ch.heigvd.iict.services.core.MessageStatus
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.repositories.OutboundMessageRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@ApplicationScoped
class PayloadService(
    private val sealer: IMessageSealer,
    private val unsealer: IMessageUnsealer,
    private val em : EntityManager,
    private val beaconRepository: BeaconRepository,
    private val outboundMessageRepository: OutboundMessageRepository,
    private val deliveryRepository: MessageDeliveryRepository
) {

    companion object {
        const val MSG_TYPE_REQ: UByte = 0x01u
        const val OP_TYPE_GENERIC_COMMAND: UByte = 0x01u
    }

    @Transactional
    fun createOutboundMessage(
        beaconId: Int,
        command: JsonObject,
        opType: UByte,
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
            msgType = MSG_TYPE_REQ, // This is a request
            opType = opType,        // This is the specific operation code
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
        // The service logic is now much cleaner.
        // We ask the repository to find and lock a job for us.
        val claimedMessage = outboundMessageRepository.findAndClaimAvailableJob(phone)
            ?: return emptyList() // No jobs available or claim failed

        // The repository has returned a locked entity. Now we finish the business logic.
        val delivery = MessageDelivery().apply {
            this.outboundMessage = claimedMessage
            this.phone = phone
            this.ackStatus = AckStatus.PENDING_ACK
        }
        delivery.persist()

        claimedMessage.deliveryCount++
        claimedMessage.status = MessageStatus.DELIVERING
        claimedMessage.persist()

        val dto = PhonePayloadDto(
            deliveryId = delivery.id!!,
            beaconId = claimedMessage.beacon.beaconId,
            encryptedBlob = claimedMessage.encryptedBlob!!.asUByteArray()
        )

        // The transaction commits here, releasing the lock.
        return listOf(dto)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Transactional
    fun processAck(request: AckRequestDto) {
        // 1. Find the specific delivery record this ACK corresponds to.
        val delivery = deliveryRepository.findById(request.deliveryId)
            ?: throw NotFoundException("Delivery record with ID ${request.deliveryId} not found.")

        val message = delivery.outboundMessage

        // This prevents a race condition where two ACKs for the same message are processed simultaneously.
        em.lock(message, LockModeType.PESSIMISTIC_WRITE)

        // If the message is already marked as acknowledged, we're done. Just log and exit.
        if (message.status == MessageStatus.ACKNOWLEDGED) {
            return
        }

        // Update the specific delivery record
        delivery.rawAckBlob = request.ackBlob.asByteArray()
        delivery.ackReceivedAt = java.time.Instant.now()
        // TODO: Decrypt the blob to determine if it's an ACK or ERR
        // For now, we'll assume it's an ACK.
        delivery.ackStatus = AckStatus.ACK_RECEIVED
        delivery.persist()

        // Update the parent OutboundMessage
        message.status = MessageStatus.ACKNOWLEDGED
        message.firstAcknowledgedAt = delivery.ackReceivedAt
        message.persist()

        // DECRYPTION (Optional but recommended)
        // You would now use the unsealer to decrypt the ackBlob and inspect its contents.
        // val sealed = SealedMessage.fromBlob(request.ackBlob.asByteArray())
        // val plaintextAck = unsealer.unseal(sealed, message.beacon)
        // Log plaintextAck.opType, etc.
    }
}