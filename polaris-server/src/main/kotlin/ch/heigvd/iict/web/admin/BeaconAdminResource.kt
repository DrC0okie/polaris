package ch.heigvd.iict.web.admin

import ch.heigvd.iict.dto.admin.BeaconAdminDto
import ch.heigvd.iict.entities.OutboundMessage
import ch.heigvd.iict.repositories.OutboundMessageRepository
import ch.heigvd.iict.services.admin.BeaconAdminService
import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant
import ch.heigvd.iict.util.PoLUtils.toHexString
import ch.heigvd.iict.web.admin.forms.BeaconFormData
import ch.heigvd.iict.web.admin.handlers.BeaconAdminFormHandler
import ch.heigvd.iict.web.admin.handlers.BeaconAdminViewRenderer
import ch.heigvd.iict.web.admin.handlers.FormProcessingResult

@Path("/admin/beacons")
@ApplicationScoped
class BeaconAdminResource(
    private val beaconAdminService: BeaconAdminService,
    private val outboundMessageRepository: OutboundMessageRepository,
    private val formHandler: BeaconAdminFormHandler,
    private val viewRenderer: BeaconAdminViewRenderer
) {

    @CheckedTemplate
    object Templates {
        @JvmStatic
        external fun beacons( beacons: List<BeaconAdminDto>, payloads: List<OutboundMessage>): TemplateInstance

        @JvmStatic
        external fun beacon_add_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance

        @JvmStatic
        external fun beacon_edit_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance
    }

    @GET
    @OptIn(ExperimentalUnsignedTypes::class)
    @Produces(MediaType.TEXT_HTML)
    fun listBeacons(): TemplateInstance {
        // This is a simple read operation, no handler needed.
        val beaconDtos = beaconAdminService.listAllBeacons().map {
            BeaconAdminDto(
                it.id,
                it.beaconId,
                it.name,
                it.locationDescription,
                it.publicKey.asUByteArray().toHexString(),
                it.publicKeyX25519?.asUByteArray()?.toHexString(),
                it.lastKnownCounter,
                it.createdAt,
                it.updatedAt
            )
        }
        // Fetch all outbound messages, sorted by creation date
        val payloads = outboundMessageRepository.listAll(Sort.by("createdAt").descending())
        return Templates.beacons(beaconDtos, payloads)
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    fun newBeaconForm(): TemplateInstance {
        // Simple view rendering.
        val emptyBeaconDto = BeaconAdminDto(null, 0, "", "", "", "", 0L, Instant.now(), Instant.now())
        return Templates.beacon_add_form(emptyBeaconDto, null)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @GET
    @Path("/edit/{id}")
    @Produces(MediaType.TEXT_HTML)
    fun editBeaconForm(@PathParam("id") id: Long): Response {
        val beacon = beaconAdminService.findBeaconById(id)
            ?: return viewRenderer.renderEditFormWithError(null, "Beacon with ID $id not found.")

        // Convert to DTO and render the template
        val beaconDto = BeaconAdminDto(
            beacon.id, beacon.beaconId, beacon.name, beacon.locationDescription,
            beacon.publicKey.asUByteArray().toHexString(),
            beacon.publicKeyX25519?.asUByteArray()?.toHexString() ?: "",
            beacon.lastKnownCounter, beacon.createdAt, beacon.updatedAt
        )
        return Response.ok(Templates.beacon_edit_form(beaconDto, null)).build()
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun createBeacon(
        // Use @FormParam for all fields to create the formData object
        @BeanParam formData: BeaconFormDataBean
    ): Response {
        val formDataObj = formData.toBeaconFormData()
        return when (val result = formHandler.processCreateBeacon(formDataObj)) {
            is FormProcessingResult.Success -> result.redirectResponse
            is FormProcessingResult.Failure -> result.errorResponse
        }
    }

    @POST
    @Path("/update/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun updateBeacon(
        @PathParam("id") id: Long,
        @BeanParam formData: BeaconFormDataBean
    ): Response {
        val formDataObj = formData.toBeaconFormData()
        return when (val result = formHandler.processUpdateBeacon(id, formDataObj)) {
            is FormProcessingResult.Success -> result.redirectResponse
            is FormProcessingResult.Failure -> result.errorResponse
        }
    }

    @POST
    @Path("/delete/{id}")
    fun deleteBeacon(@PathParam("id") id: Long): Response {
        return when (val result = formHandler.processDeleteBeacon(id)) {
            is FormProcessingResult.Success -> result.redirectResponse
            is FormProcessingResult.Failure -> result.errorResponse
        }
    }
}

// Helper JAX-RS bean to automatically map form params to an object
class BeaconFormDataBean {
    @FormParam("beaconId")
    var technicalId: String? = null

    @FormParam("name")
    var name: String? = null

    @FormParam("locationDescription")
    var locationDescription: String? = null

    @FormParam("publicKeyHex")
    var publicKeyEd25519Hex: String? = null

    @FormParam("publicKeyX25519Hex")
    var publicKeyX25519Hex: String? = null

    fun toBeaconFormData() =
        BeaconFormData(technicalId, name, locationDescription, publicKeyEd25519Hex, publicKeyX25519Hex)
}