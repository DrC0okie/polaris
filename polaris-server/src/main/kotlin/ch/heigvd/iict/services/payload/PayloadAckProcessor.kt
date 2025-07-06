package ch.heigvd.iict.services.payload

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.AckResponseDto
import ch.heigvd.iict.entities.MessageDelivery
import ch.heigvd.iict.repositories.MessageDeliveryRepository
import ch.heigvd.iict.services.admin.BeaconAdminService
import ch.heigvd.iict.services.crypto.X25519SharedKeyManager
import ch.heigvd.iict.services.protocol.AckStatus
import ch.heigvd.iict.services.protocol.MessageStatus
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.services.protocol.MessageType
import ch.heigvd.iict.services.protocol.OperationType
import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import kotlinx.serialization.json.JsonObject
import java.time.Instant

@ApplicationScoped
@Transactional
class PayloadAckProcessor(
    private val deliveryRepository: MessageDeliveryRepository,
    private val unsealer: IMessageUnsealer,
    private val entityManager: EntityManager,
    private val beaconAdminService: BeaconAdminService,
    private val payloadService: PayloadService,
    private val keyManager: X25519SharedKeyManager
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

    private fun processDecryptedAck(plaintext: PlaintextMessage, delivery: MessageDelivery) {
        val message = delivery.outboundMessage
        val beacon = message.beacon

        if (plaintext.beaconCounter > beacon.lastKnownCounter) {
            beacon.lastKnownCounter = plaintext.beaconCounter
            beacon.persist()
        }

        if (message.opType == OperationType.ROTATE_KEY_INIT && plaintext.msgType == MessageType.ACK) {
            handleRotateInitAck(plaintext, delivery)
        } else if (message.opType == OperationType.ROTATE_KEY_FINISH && plaintext.msgType == MessageType.ACK) {
            handleRotateFinishAck(delivery)
        } else {
            handleGenericAck(plaintext, delivery)
        }
    }

    private fun handleRotateInitAck(plaintext: PlaintextMessage, delivery: MessageDelivery) {
        Log.info("Processing ACK for RotateKeyInit from beacon ${delivery.outboundMessage.beacon.id}")
        val originalMessage = delivery.outboundMessage
        val beacon = originalMessage.beacon

        // Extract new key from payload
        val newPublicKey = plaintext.payload
        if (newPublicKey.size != 32) {
            Log.error("Invalid key size in RotateKeyInit ACK payload.")
            delivery.ackStatus = AckStatus.PROCESSING_ERROR
            originalMessage.status = MessageStatus.FAILED
            return
        }

        // Update key in db
        beaconAdminService.updateBeaconX25519Key(beacon.id!!, newPublicKey)

        // 3. Invalidate cache to force new derivation
        keyManager.invalidateCacheForBeacon(beacon)

        // Mark this job as AKC, but not global process
        delivery.ackStatus = AckStatus.ACK_RECEIVED
        originalMessage.status = MessageStatus.ACKNOWLEDGED // init job is finished
        originalMessage.firstAcknowledgedAt = delivery.ackReceivedAt

        // Create the next job
        Log.info("Creating RotateKeyFinish job for beacon ${beacon.id}")
        payloadService.createOutboundMessage(
            beaconId = beacon.beaconId,
            command = JsonObject(emptyMap()),
            opType = OperationType.ROTATE_KEY_FINISH
        )
    }

    private fun handleRotateFinishAck(delivery: MessageDelivery) {
        Log.info("Processing final ACK for key rotation for beacon ${delivery.outboundMessage.beacon.id}")
        delivery.ackStatus = AckStatus.ACK_RECEIVED
        delivery.outboundMessage.status = MessageStatus.ACKNOWLEDGED
        delivery.outboundMessage.firstAcknowledgedAt = delivery.ackReceivedAt
    }

    private fun handleGenericAck(plaintext: PlaintextMessage, delivery: MessageDelivery) {
        when (plaintext.msgType) {
            MessageType.ACK -> {
                delivery.ackStatus = AckStatus.ACK_RECEIVED
                delivery.outboundMessage.status = MessageStatus.ACKNOWLEDGED
                delivery.outboundMessage.firstAcknowledgedAt = delivery.ackReceivedAt
            }

            MessageType.ERR -> {
                delivery.ackStatus = AckStatus.ERR_RECEIVED
                delivery.outboundMessage.status = MessageStatus.FAILED
            }

            else -> {
                Log.warn("Unhandled message type in generic ACK handler: ${plaintext.msgType}")
                delivery.ackStatus = AckStatus.PROCESSING_ERROR
                delivery.outboundMessage.status = MessageStatus.FAILED
            }
        }
    }
}