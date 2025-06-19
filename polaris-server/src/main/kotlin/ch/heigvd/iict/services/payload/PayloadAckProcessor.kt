package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.AckResponseDto
import ch.heigvd.iict.entities.MessageDelivery
import ch.heigvd.iict.repositories.MessageDeliveryRepository
import ch.heigvd.iict.services.protocol.AckStatus
import ch.heigvd.iict.services.protocol.MessageStatus
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.services.protocol.MessageType
import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.ws.rs.NotFoundException
import java.time.Instant

@ApplicationScoped
class PayloadAckProcessor(
    private val deliveryRepository: MessageDeliveryRepository,
    private val unsealer: IMessageUnsealer,
    private val entityManager: EntityManager
) {

    @OptIn(ExperimentalUnsignedTypes::class)
    fun process(request: AckRequestDto): AckResponseDto {
        val delivery = deliveryRepository.findById(request.deliveryId)
            ?: throw NotFoundException("Delivery record with ID ${request.deliveryId} not found.")

        // We acquire the lock on the parent message.
        val message = delivery.outboundMessage
        entityManager.lock(message, LockModeType.PESSIMISTIC_WRITE)

        // If already acknowledged, do nothing.
        if (message.status == MessageStatus.ACKNOWLEDGED || message.status == MessageStatus.FAILED) {
            Log.info("Ignoring redundant ACK for already completed message job ${message.id}")
            return AckResponseDto(
                deliveryId = delivery.id!!,
                status = MessageStatus.REDUNDANT.name,
                message = "ACK was ignored as the job was already completed."
            )
        }

        // Store the raw blob regardless of validation outcome.
        delivery.rawAckBlob = request.ackBlob.asByteArray()
        delivery.ackReceivedAt = Instant.now()

        try {
            // Attempt to unseal the message
            val sealedAck = SealedMessage.fromBlob(request.ackBlob.asByteArray())
            val plaintextAck = unsealer.unseal(sealedAck, message.beacon)

            // Validate and process the decrypted plaintext
            processDecryptedAck(plaintextAck, delivery)

        } catch (e: AeadCorrupedOrTamperedDataException) {
            // Decryption failed (bad tag)
            Log.warn("ACK decryption failed for delivery ${delivery.id}. Bad tag or corrupted data.", e)
            delivery.ackStatus = AckStatus.FAILED_DECRYPTION
            message.status = MessageStatus.FAILED
        } catch (e: Exception) {
            // Other errors (e.g., parsing, unexpected plaintext)
            Log.warn("ACK validation failed for delivery ${delivery.id}: ${e.message}")
            delivery.ackStatus = AckStatus.PROCESSING_ERROR
            message.status = MessageStatus.FAILED
        }

        delivery.persist()
        message.persist()

        return AckResponseDto(
            deliveryId = delivery.id!!,
            status = delivery.ackStatus.name,
            message = "Processed ACK for delivery ${delivery.id}. Status: ${delivery.ackStatus.name}"
        )
    }

    private fun processDecryptedAck(plaintext: PlaintextMessage, delivery: MessageDelivery){
        val message = delivery.outboundMessage
        val beacon = message.beacon

        if (plaintext.beaconCounter > beacon.lastKnownCounter) {
            beacon.lastKnownCounter = plaintext.beaconCounter
            beacon.persist()
        }

        when (plaintext.msgType) {
            MessageType.ACK -> {
                delivery.ackStatus = AckStatus.ACK_RECEIVED
                message.status = MessageStatus.ACKNOWLEDGED
                message.firstAcknowledgedAt = delivery.ackReceivedAt
            }
            MessageType.ERR -> {
                delivery.ackStatus = AckStatus.ERR_RECEIVED
                message.status = MessageStatus.FAILED
            }
            else -> {
                throw IllegalStateException("Invalid message type received from beacon: ${plaintext.msgType}")
            }
        }
    }
}