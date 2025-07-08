package ch.heigvd.iict.web.admin

import ch.heigvd.iict.dto.admin.BeaconAdminDto
import ch.heigvd.iict.dto.admin.PhoneAdminDto
import ch.heigvd.iict.dto.admin.TokenAdminDto
import ch.heigvd.iict.entities.InboundMessage
import ch.heigvd.iict.entities.OutboundMessage
import ch.heigvd.iict.repositories.InboundMessageRepository
import ch.heigvd.iict.repositories.OutboundMessageRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import ch.heigvd.iict.repositories.RegisteredPhoneRepository
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

/**
 * JAX-RS resource for the beacon administration web interface.
 *
 * This class exposes endpoints for listing, creating, editing, and deleting beacons
 * via a server-rendered HTML interface using Qute templates.
 */
@Path("/admin/dashboard")
@ApplicationScoped
class DashboardAdminResource(
    private val beaconAdminService: BeaconAdminService,
    private val outboundMessageRepository: OutboundMessageRepository,
    private val registeredPhoneRepository: RegisteredPhoneRepository,
    private val tokenRecordRepository: PoLTokenRecordRepository,
    private val inboundMessageRepository: InboundMessageRepository,
    private val formHandler: BeaconAdminFormHandler,
    private val viewRenderer: BeaconAdminViewRenderer
) {

    /**
     * Defines the Qute templates used by this resource.
     */
    @CheckedTemplate
    object Templates {

        @JvmStatic
        external fun dashboard(
            beacons: List<BeaconAdminDto>,
            payloads: List<OutboundMessage>,
            inboundMessages: List<InboundMessage>,
            phones: List<PhoneAdminDto>,
            tokens: List<TokenAdminDto>
        ): TemplateInstance

        @JvmStatic
        external fun beacon_add_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance

        @JvmStatic
        external fun beacon_edit_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance
    }

    /**
     * [GET] /admin/dashboard
     * Displays the main dashboard, listing all registered beacons and outbound message jobs.
     * @return A Qute [TemplateInstance] to render the dashboard view.
     */
    @GET
    @OptIn(ExperimentalUnsignedTypes::class)
    @Produces(MediaType.TEXT_HTML)
    fun getDashboard(): TemplateInstance {

        // Fetch Beacons
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
        // Fetch Outbound Payloads
        val payloads = outboundMessageRepository.listAll(Sort.by("createdAt").descending())

        // Fetch Inbound Messages
        val inboundMessages = inboundMessageRepository.listAll(Sort.by("receivedAt").descending())

        // Fetch Registered Phones and map to DTO
        val phoneDtos = registeredPhoneRepository.listAll(Sort.by("createdAt").descending()).map {
            PhoneAdminDto(
                it.id,
                it.publicKey.toHexString(),
                it.apiKey.substring(0, 8) + "...",
                it.userAgent,
                it.lastSeenAt,
                it.createdAt
            )
        }

        // Fetch PoL Tokens and map to DTO
        val tokenDtos = tokenRecordRepository.listAll(Sort.by("receivedAt").descending()).map {
            TokenAdminDto(
                it.id,
                it.phone.id,
                it.beacon.beaconId,
                it.beaconCounter,
                it.nonceHex,
                it.isValid,
                it.validationError,
                it.receivedAt
            )
        }

        return Templates.dashboard(beaconDtos, payloads, inboundMessages, phoneDtos, tokenDtos)
    }

    /**
     * [GET] /admin/dashboard/beacons/new
     * Displays the form for creating a new beacon.
     * @return A Qute [TemplateInstance] to render the "add beacon" form.
     */
    @GET
    @Path("/beacons/new")
    @Produces(MediaType.TEXT_HTML)
    fun newBeaconForm(): TemplateInstance {
        // Simple view rendering.
        val emptyBeaconDto = BeaconAdminDto(null, 0, "", "", "", "", 0L, Instant.now(), Instant.now())
        return Templates.beacon_add_form(emptyBeaconDto, null)
    }

    /**
     * [GET] /admin/dashboard/beacons/edit/{id}
     * Displays the form for editing an existing beacon, pre-populated with its data.
     * @param id The database ID of the beacon to edit.
     * @return A JAX-RS [Response] containing the rendered form or an error page if not found.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @GET
    @Path("/beacons/edit/{id}")
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

    /**
     * [POST] /admin/dashboard/beacons/create
     * Processes the submission of the "add beacon" form.
     * Delegates logic to the [BeaconAdminFormHandler].
     * @param formData A JAX-RS bean that automatically maps the submitted form fields.
     * @return A redirect response on success, or a response containing the form with errors on failure.
     */
    @POST
    @Path("/beacons/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun createBeacon(@BeanParam formData: BeaconFormDataBean): Response {
        val formDataObj = formData.toBeaconFormData()
        return when (val result = formHandler.processCreateBeacon(formDataObj)) {
            is FormProcessingResult.Success -> result.redirectResponse
            is FormProcessingResult.Failure -> result.errorResponse
        }
    }

    /**
     * [POST] /admin/dashboard/beacons/update/{id}
     * Processes the submission of the "edit beacon" form.
     * Delegates logic to the [BeaconAdminFormHandler].
     * @param id The database ID of the beacon being updated.
     * @param formData A JAX-RS bean that automatically maps the submitted form fields.
     * @return A redirect response on success, or a response containing the form with errors on failure.
     */
    @POST
    @Path("/beacons/update/{id}")
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

    /**
     * [POST] /admin/dashboard/beacons/delete/{id}
     * Processes a request to delete a beacon.
     * Delegates logic to the [BeaconAdminFormHandler].
     * @param id The database ID of the beacon to delete.
     * @return A redirect response on success.
     */
    @POST
    @Path("/beacons/delete/{id}")
    fun deleteBeacon(@PathParam("id") id: Long): Response {
        return when (val result = formHandler.processDeleteBeacon(id)) {
            is FormProcessingResult.Success -> result.redirectResponse
            is FormProcessingResult.Failure -> result.errorResponse
        }
    }
}

/**
 * A JAX-RS helper bean to simplify binding of `x-www-form-urlencoded` data to a structured object.
 */
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