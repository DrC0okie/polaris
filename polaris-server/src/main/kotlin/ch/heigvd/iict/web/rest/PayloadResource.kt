package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.AckRequestDto
import ch.heigvd.iict.dto.api.BeaconPayloadDto
import ch.heigvd.iict.dto.api.PayloadListDto
import ch.heigvd.iict.dto.api.RawDataDto
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.services.payload.PayloadService
import ch.heigvd.iict.web.rest.auth.Secured
import jakarta.enterprise.context.RequestScoped
import jakarta.validation.Valid
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.container.ContainerRequestContext

/**
 * JAX-RS resource for managing the secure payload data mule channel.
 *
 * All endpoints in this resource are secured and require a valid API key.
 *
 * @property payloadService The service that orchestrates all payload-related business logic.
 */
@Path("/api/v1/payloads")
@RequestScoped
@Secured
class PayloadResource(
    private val payloadService: PayloadService
) {
    /** Injects the JAX-RS request context to access properties set by filters (e.g., the authenticated phone). */
    @Context
    private lateinit var requestContext: ContainerRequestContext

    /**
     * [GET] /api/v1/payloads
     * Allows a phone to fetch pending outbound message jobs destined for beacons.
     * @return A [Response] containing a list of payload jobs for the phone to deliver.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getPendingPayloads(): Response {
        val phone = requestContext.getProperty("authenticatedPhone") as RegisteredPhone
        val payloads = payloadService.getPendingPayloadsForPhone(phone)
        val responseWrapper = PayloadListDto(payloads)
        return Response.ok(responseWrapper).build()
    }

    /**
     * [POST] /api/v1/payloads
     * Allows a phone to submit an inbound payload that was initiated by a beacon.
     * @param request A DTO containing the beacon's ID and its encrypted data blob.
     * @return A [Response] containing the server's encrypted ACK to be relayed back to the beacon.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submitInboundPayload(@Valid request: BeaconPayloadDto): Response {
        val responseBlob: RawDataDto = payloadService.processInboundPayload(request)

        return Response.ok(responseBlob).build()
    }

    /**
     * [POST] /api/v1/payloads/ack
     * Allows a phone to submit an acknowledgment blob from a beacon for a previously delivered job.
     * @param request A DTO containing the original delivery ID and the beacon's ACK/ERR blob.
     * @return A [Response] confirming the processing of the acknowledgment.
     */
    @POST
    @Path("/ack")
    @Consumes(MediaType.APPLICATION_JSON)
    fun submitAck(@Valid request: AckRequestDto): Response {
        val result = payloadService.processAck(request)
        return Response.ok(result).build()
    }
}