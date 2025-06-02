package ch.heigvd.iict.web.admin

import ch.heigvd.iict.dto.admin.BeaconAdminDto
import ch.heigvd.iict.services.admin.BeaconAdminService
import ch.heigvd.iict.util.PoLUtils
import io.quarkus.logging.Log
import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import java.time.Instant
import ch.heigvd.iict.util.PoLUtils.toHexString

@Path("/admin/beacons")
@ApplicationScoped
// TODO: Ajouter la sécurité ici plus tard avec @RolesAllowed("ADMIN")
@OptIn(ExperimentalUnsignedTypes::class)
class BeaconAdminResource {

    @Inject
    private lateinit var beaconAdminService: BeaconAdminService

    @CheckedTemplate
    object Templates {
        @JvmStatic
        external fun beacons(beacons: List<BeaconAdminDto>): TemplateInstance

        @JvmStatic
        external fun beacon_add_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance

        @JvmStatic
        external fun beacon_edit_form(beacon: BeaconAdminDto?, errorMessage: String? = null): TemplateInstance
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun listBeacons(): TemplateInstance {
        val beaconDtos = beaconAdminService.listAllBeacons().map {
            BeaconAdminDto(
                it.id,
                it.beaconId,
                it.name,
                it.locationDescription,
                it.publicKey.asUByteArray().toHexString(),
                it.lastKnownCounter,
                it.createdAt,
                it.updatedAt
            )
        }
        return Templates.beacons(beaconDtos)
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    fun newBeaconForm(): TemplateInstance {
        // Crée un DTO vide pour le formulaire d'ajout
        val emptyBeaconDto = BeaconAdminDto(null, 0, "", "", "", 0L, Instant.now(), Instant.now())
        return Templates.beacon_add_form(emptyBeaconDto, null) // Passe un DTO non-nul
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    fun createBeacon(
        @FormParam("beaconId") technicalId: Int,
        @FormParam("name") name: String,
        @FormParam("locationDescription") locationDescription: String,
        @FormParam("publicKeyHex") publicKeyHex: String
    ): Response {
        try {
            if (publicKeyHex.length != 64 || !publicKeyHex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                val formData = BeaconAdminDto(
                    null,
                    technicalId,
                    name,
                    locationDescription,
                    publicKeyHex,
                    0L,
                    Instant.now(),
                    Instant.now()
                )
                // Redirige vers le formulaire d'ajout avec message d'erreur
                return Response.ok(Templates.beacon_add_form(formData, "Public key must be 64 hex characters.")).build()
            }
            val publicKeyBytes = PoLUtils.hexStringToUByteArray(publicKeyHex).asByteArray()
            beaconAdminService.addBeacon(technicalId, name, locationDescription, publicKeyBytes)
            Log.info("Beacon created with ID: $technicalId")
            return Response.seeOther(UriBuilder.fromPath("/admin/beacons").build()).build()
        } catch (e: IllegalArgumentException) {
            Log.warn("Failed to create beacon: ${e.message}")
            val formData = BeaconAdminDto(
                null,
                technicalId,
                name,
                locationDescription,
                publicKeyHex,
                0L,
                Instant.now(),
                Instant.now()
            )
            return Response.ok(Templates.beacon_add_form(formData, e.message)).status(Response.Status.BAD_REQUEST)
                .build()
        } catch (e: Exception) {
            Log.error("Error creating beacon", e)
            val formData = BeaconAdminDto(
                null,
                technicalId,
                name,
                locationDescription,
                publicKeyHex,
                0L,
                Instant.now(),
                Instant.now()
            )
            return Response.ok(Templates.beacon_add_form(formData, "An unexpected error occurred."))
                .status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }
    }

    // TODO: Implémenter les méthodes pour Edit (GET pour le formulaire, POST pour la mise à jour) et Delete (POST)
    @GET
    @Path("/edit/{id}")
    @Produces(MediaType.TEXT_HTML)
    fun editBeaconForm(@PathParam("id") id: Long): TemplateInstance {
        val beacon = beaconAdminService.findBeaconById(id)
        val beaconDto = beacon?.let {
            BeaconAdminDto(
                it.id,
                it.beaconId,
                it.name,
                it.locationDescription,
                it.publicKey.asUByteArray().toHexString(),
                it.lastKnownCounter,
                it.createdAt,
                it.updatedAt
            )
        }
        if (beaconDto == null) {
            return Templates.beacon_edit_form(null, "Beacon not found with ID: $id")
        }
        return Templates.beacon_edit_form(beaconDto)
    }

    @POST
    @Path("/update/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    fun updateBeacon(
        @PathParam("id") id: Long,
        @FormParam("name") name: String,
        @FormParam("locationDescription") locationDescription: String
    ): Response {
        try {
            val updatedBeacon = beaconAdminService.updateBeacon(id, name, locationDescription)
            if (updatedBeacon == null) {
                val errorDto = BeaconAdminDto(
                    id,
                    0,
                    name,
                    locationDescription,
                    "",
                    0L,
                    Instant.now(),
                    Instant.now()
                ) // DTO partiel pour l'erreur
                return Response.ok(Templates.beacon_edit_form(errorDto, "Beacon not found for update."))
                    .status(Response.Status.NOT_FOUND).build()
            }
            Log.info("Beacon updated with DB ID: $id")
            return Response.seeOther(UriBuilder.fromPath("/admin/beacons").build()).build()
        } catch (e: Exception) {
            Log.error("Error updating beacon $id", e)
            val existingBeacon = beaconAdminService.findBeaconById(id) // Recharger pour l'affichage
            val errorDto = existingBeacon?.let {
                BeaconAdminDto(
                    it.id,
                    it.beaconId,
                    name,
                    locationDescription,
                    it.publicKey.asUByteArray().toHexString(),
                    it.lastKnownCounter,
                    it.createdAt,
                    it.updatedAt
                )
            }
            return Response.ok(Templates.beacon_edit_form(errorDto, "Failed to update: ${e.message}"))
                .status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }
    }


    @POST
    @Path("/delete/{id}")
    @Transactional
    fun deleteBeacon(@PathParam("id") id: Long): Response {
        beaconAdminService.deleteBeacon(id)
        Log.info("Beacon deleted with DB ID: $id")
        return Response.seeOther(UriBuilder.fromPath("/admin/beacons").build()).build()
    }
}