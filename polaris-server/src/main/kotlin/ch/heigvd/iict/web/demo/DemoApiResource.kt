package ch.heigvd.iict.web.demo

import ch.heigvd.iict.repositories.*
import ch.heigvd.iict.services.payload.PayloadService
import ch.heigvd.iict.services.protocol.OperationType
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

    object InstantSerializer : KSerializer<Instant> {
        override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Instant) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): Instant {
            return Instant.parse(decoder.decodeString())
        }
    }

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

            Response.ok("Job de rotation des clés créé pour la balise #${beacon.id} (${beacon.name}).")
                .build()

        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(e.message ?: "An unknown error occurred.")
                .build()
        }
    }
}