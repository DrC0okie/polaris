package ch.heigvd.iict.web.admin

import ch.heigvd.iict.dto.admin.PayloadCreationDto
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.services.payload.PayloadService
import io.quarkus.logging.Log
import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Path("/admin/payloads")
@ApplicationScoped
class PayloadAdminResource(
    private val payloadService: PayloadService,
    private val beaconRepository: BeaconRepository
) {

    @CheckedTemplate
    object Templates {
        @JvmStatic
        external fun payload_form(dto: PayloadCreationDto, successMessage: String? = null, errorMessage: String? = null): TemplateInstance
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    fun newPayloadForm(): TemplateInstance {
        val beacons = beaconRepository.listAll()
        val dto = PayloadCreationDto(beacons, null, null, "{}", 1)
        return Templates.payload_form(dto)
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    fun createPayload(
        @FormParam("beaconId") beaconId: Int,
        @FormParam("opType") opType: Short,
        @FormParam("commandPayload") commandPayload: String,
        @FormParam("redundancyFactor") redundancyFactor: Int
    ): Response {
        var errorMessage: String? = null
        try {
            // Validate JSON
            val commandJson = Json.parseToJsonElement(commandPayload) as JsonObject

            // Create the job
            val message = payloadService.createOutboundMessage(
                beaconId = beaconId,
                command = commandJson,
                opType = opType.toUByte(),
                redundancyFactor = redundancyFactor
            )
            Log.info("Successfully created job with ID ${message.id} for beacon $beaconId.")

            // Redirect to the main list where the user can see their new job.
            return Response.seeOther(UriBuilder.fromPath("/admin/beacons").build()).build()

        } catch (e: Exception) {
            errorMessage = "Failed to create payload: ${e.message}"
            Log.error("Error creating payload", e)

            // On failure, re-render the form with the error message
            val beacons = beaconRepository.listAll()
            val dto = PayloadCreationDto(beacons, beaconId, opType.toInt(), commandPayload, redundancyFactor)
            val template = Templates.payload_form(dto, null, errorMessage)
            return Response.ok(template).build()
        }
    }
}