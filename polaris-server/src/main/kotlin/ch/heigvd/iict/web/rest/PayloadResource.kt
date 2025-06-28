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

@Path("/api/v1/payloads")
@RequestScoped
@Secured
class PayloadResource(
    private val payloadService: PayloadService
) {
    @Context
    private lateinit var requestContext: ContainerRequestContext

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getPendingPayloads(): Response {
        val phone = requestContext.getProperty("authenticatedPhone") as RegisteredPhone
        val payloads = payloadService.getPendingPayloadsForPhone(phone)
        val responseWrapper = PayloadListDto(payloads)
        return Response.ok(responseWrapper).build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submitInboundPayload(@Valid request: BeaconPayloadDto): Response {
        val responseBlob: RawDataDto = payloadService.processInboundPayload(request)

        return Response.ok(responseBlob).build()
    }

    @POST
    @Path("/ack")
    @Consumes(MediaType.APPLICATION_JSON)
    fun submitAck(@Valid request: AckRequestDto): Response {
        val result = payloadService.processAck(request)
        return Response.ok(result).build()
    }
}