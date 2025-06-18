package ch.heigvd.iict.web.admin.handlers

import ch.heigvd.iict.dto.admin.BeaconAdminDto
import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.web.admin.BeaconAdminResource
import ch.heigvd.iict.web.admin.forms.BeaconFormData
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import java.time.Instant
import com.ionspin.kotlin.crypto.util.toHexString

@ApplicationScoped
class BeaconAdminViewRenderer {

    fun renderAddFormWithError(formData: BeaconFormData, errorMessage: String): Response {
        // Convert the raw form data back to a DTO to re-populate the form
        val dto = BeaconAdminDto(
            id = null,
            technicalId = formData.technicalId?.toIntOrNull() ?: 0,
            name = formData.name ?: "",
            locationDescription = formData.locationDescription ?: "",
            publicKeyHex = formData.publicKeyEd25519Hex ?: "",
            publicKeyX25519Hex = formData.publicKeyX25519Hex ?: "",
            lastKnownCounter = 0L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val templateInstance = BeaconAdminResource.Templates.beacon_add_form(dto, errorMessage)
        return Response.status(Response.Status.BAD_REQUEST).entity(templateInstance).build()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun renderEditFormWithError(beacon: Beacon?, errorMessage: String): Response {
        // Convert the entity to a DTO for rendering
        val dto = beacon?.let {
            BeaconAdminDto(
                id = it.id,
                technicalId = it.beaconId,
                name = it.name,
                locationDescription = it.locationDescription,
                publicKeyHex = it.publicKey.asUByteArray().toHexString(),
                publicKeyX25519Hex = it.publicKeyX25519?.asUByteArray()?.toHexString() ?: "",
                lastKnownCounter = it.lastKnownCounter,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val status = if (beacon == null) Response.Status.NOT_FOUND else Response.Status.BAD_REQUEST
        val templateInstance = BeaconAdminResource.Templates.beacon_edit_form(dto, errorMessage)
        return Response.status(status).entity(templateInstance).build()
    }
}