package ch.heigvd.iict.web.demo

import ch.heigvd.iict.repositories.*
import ch.heigvd.iict.services.payload.PayloadService
import ch.heigvd.iict.services.protocol.OperationType
import ch.heigvd.iict.util.InstantSerializer
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable
import java.time.Instant

@Path("/demo/api")
@Produces(MediaType.APPLICATION_JSON)
class DemoApiResource {

    @Inject
    lateinit var beaconRepo: BeaconRepository
    @Inject
    lateinit var tokenRepo: PoLTokenRecordRepository
    @Inject
    lateinit var outboundRepo: OutboundMessageRepository
    @Inject
    lateinit var payloadService: PayloadService
    @Inject
    lateinit var inboundRepo: InboundMessageRepository

    @Serializable
    data class SummaryDto(
        @Serializable(with = InstantSerializer::class)
        val now: Instant,
        val beacons: Long,
        val tokensTotal: Long,
        val tokensValid: Long,
        val tokensInvalid: Long,
        val outboundPending: Long,
        val outboundDelivered: Long,
        val inboundMessages: Long
    )

    @GET
    @Path("/summary")
    fun summary(): SummaryDto {
        val beacons = beaconRepo.count()
        val tokensTotal = tokenRepo.count()
        val tokensValid = tokenRepo.count("isValid", true)
        val tokensInvalid = tokenRepo.count("isValid", false)
        val outboundPending = outboundRepo.count("status in ?1", listOf("PENDING", "DELIVERING"))
        val outboundDelivered = outboundRepo.count("firstAcknowledgedAt is not null")
        val inboundMessages = inboundRepo.count()
        return SummaryDto(
            Instant.now(),
            beacons,
            tokensTotal,
            tokensValid,
            tokensInvalid,
            outboundPending,
            outboundDelivered,
            inboundMessages
        )
    }

    @Serializable
    data class TokenLiteDto(
        val id: Long,
        val beaconId: Long,
        val beaconName: String,
        val phoneId: Long,
        val counter: Long,
        val isValid: Boolean,
        @Serializable(with = InstantSerializer::class)
        val receivedAt: Instant
    )

    @GET
    @Path("/tokens")
    fun tokens(@QueryParam("limit") @DefaultValue("50") limit: Int): List<TokenLiteDto> {
        val list = tokenRepo.find("order by receivedAt desc").page(0, limit).list()
        return list.map {
            TokenLiteDto(
                it.id!!,
                it.beacon.id!!,
                it.beacon.name,
                it.phone.id!!,
                it.beaconCounter,
                it.isValid,
                it.receivedAt
            )
        }
    }

    @Serializable
    data class OutboundLiteDto(
        val id: Long,
        val beaconId: Long,
        val beaconName: String,
        val status: String,
        val opType: String,
        val redundancy: Int,
        val claimed: Int,
        @Serializable(with = InstantSerializer::class)
        val createdAt: Instant,
        @Serializable(with = InstantSerializer::class)
        val firstAckAt: Instant?
    )

    @GET
    @Path("/outbound")
    fun outbound(@QueryParam("limit") @DefaultValue("50") limit: Int): List<OutboundLiteDto> {
        val list = outboundRepo.find("order by createdAt desc").page(0, limit).list()
        return list.map {
            OutboundLiteDto(
                it.id!!,
                it.beacon.id!!,
                it.beacon.name,
                it.status.name,
                it.opType.name,
                it.redundancyFactor,
                it.deliveryCount,
                it.createdAt,
                it.firstAcknowledgedAt
            )
        }
    }

    @Serializable
    data class InboundLiteDto(
        val id: Long,
        val beaconId: Long,
        val msgType: String,
        val opType: String,
        val beaconCounter: Long,
        @Serializable(with = InstantSerializer::class)
        val receivedAt: Instant
    )

    @GET
    @Path("/inbound")
    fun inbound(@QueryParam("limit") @DefaultValue("50") limit: Int): List<InboundLiteDto> {
        val list = inboundRepo.find("order by receivedAt desc").page(0, limit).list()
        return list.map {
            InboundLiteDto(
                it.id!!,
                it.beacon.id!!,
                it.msgType.name,
                it.opType.name,
                it.beaconCounter,
                it.receivedAt
            )
        }
    }

    @Serializable
    data class BeaconLiteDto(
        val id: Long,
        val technicalId: Int,
        val name: String,
        val lastKnownCounter: Long,
        @Serializable(with = InstantSerializer::class)
        val updatedAt: Instant
    )

    @GET
    @Path("/beacons")
    fun beacons(): List<BeaconLiteDto> {
        return beaconRepo.listAll().map {
            BeaconLiteDto(
                it.id!!,
                it.beaconId,
                it.name,
                it.lastKnownCounter,
                it.updatedAt
            )
        }
    }

    @Serializable
    data class SimpleMessage(val message: String)

    @POST
    @Path("/beacons/{id}/rotate-key")
    fun rotateKey(@PathParam("id") id: Long): Response {
        return try {
            val beacon = beaconRepo.findById(id)
                ?: throw NotFoundException("Beacon with ID $id not found.")

            payloadService.createOutboundMessage(
                beacon.beaconId,
                kotlinx.serialization.json.JsonObject(emptyMap()),
                OperationType.ROTATE_KEY_INIT
            )

            Response.ok(SimpleMessage("Job de rotation des clés créé pour la balise #${beacon.id} (${beacon.name})."))
                .build()

        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(e.message ?: "An unknown error occurred.")
                .build()
        }
    }

    @Serializable
    data class DeliveryDetailDto(
        val phoneId: Long,
        @Serializable(with = InstantSerializer::class)
        val deliveredAt: Instant,
        val ackStatus: String,
        @Serializable(with = InstantSerializer::class)
        val ackReceivedAt: Instant?
    )

    @Serializable
    data class OutboundDetailDto(
        val messageId: Long,
        val beaconName: String,
        val beaconTechnicalId: Int,
        val status: String,
        val opType: String,
        val commandPayload: String,
        val redundancy: Int,
        val deliveryCount: Int,
        @Serializable(with = InstantSerializer::class)
        val createdAt: Instant,
        @Serializable(with = InstantSerializer::class)
        val firstAckAt: Instant?,
        val deliveries: List<DeliveryDetailDto>
    )

    @Serializable
    data class TokenDetailDto(
        val tokenId: Long,
        @Serializable(with = InstantSerializer::class)
        val receivedAt: Instant,
        val isValid: Boolean,
        val validationError: String?,
        // Beacon Info
        val beaconName: String,
        val beaconTechnicalId: Int,
        val beaconLastKnownCounter: Long,
        // Phone Info
        val phoneId: Long,
        val phoneUserAgent: String?,
        // Raw Data
        val nonceHex: String,
        val phonePkUsedHex: String,
        val beaconPkUsedHex: String,
        val phoneSigHex: String,
        val beaconSigHex: String
    )

    @Serializable
    data class InboundDetailDto(
        val messageId: Long,
        val beaconName: String,
        val beaconTechnicalId: Int,
        val msgType: String,
        val opType: String,
        val beaconCounter: Long,
        val payload: String?,
        @Serializable(with = InstantSerializer::class)
        val receivedAt: Instant
    )

    @GET
    @Path("/details/{type}/{id}")
    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
    fun getDetails(@PathParam("type") type: String, @PathParam("id") id: Long): Response {
        return try {
            val details = when (type) {
                "token" -> {
                    val token = tokenRepo.findById(id) ?: throw NotFoundException("Token $id not found")
                    TokenDetailDto(
                        token.id!!,
                        token.receivedAt,
                        token.isValid,
                        token.validationError,
                        token.beacon.name,
                        token.beacon.beaconId,
                        token.beacon.lastKnownCounter,
                        token.phone.id!!,
                        token.phone.userAgent,
                        token.nonceHex,
                        token.phonePkUsed.toHexString(),
                        token.beaconPkUsed.toHexString(),
                        token.phoneSig.toHexString(),
                        token.beaconSig.toHexString()
                    )
                }
                "outbound" -> {
                    val message = outboundRepo.findById(id) ?: throw NotFoundException("Outbound message $id not found")
                    // Eagerly fetch deliveries
                    val deliveries = message.deliveries.map {
                        DeliveryDetailDto(
                            it.phone.id!!,
                            it.deliveredAt,
                            it.ackStatus.name,
                            it.ackReceivedAt
                        )
                    }
                    OutboundDetailDto(
                        message.id!!,
                        message.beacon.name,
                        message.beacon.beaconId,
                        message.status.name,
                        message.opType.name,
                        message.commandPayload,
                        message.redundancyFactor,
                        message.deliveryCount,
                        message.createdAt,
                        message.firstAcknowledgedAt,
                        deliveries
                    )
                }
                "inbound" -> {
                    val message = inboundRepo.findById(id) ?: throw NotFoundException("Inbound message $id not found")
                    InboundDetailDto(
                        message.id!!,
                        message.beacon.name,
                        message.beacon.beaconId,
                        message.msgType.name,
                        message.opType.name,
                        message.beaconCounter,
                        message.payload,
                        message.receivedAt
                    )
                }
                else -> throw BadRequestException("Unknown detail type: $type")
            }
            Response.ok(details).build()
        } catch (e: Exception) {
            Response.status(Response.Status.NOT_FOUND).entity(SimpleMessage(e.message ?: "Error")).build()
        }
    }
}