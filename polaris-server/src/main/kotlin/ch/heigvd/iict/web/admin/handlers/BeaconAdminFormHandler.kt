package ch.heigvd.iict.web.admin.handlers

import ch.heigvd.iict.services.admin.BeaconAdminService
import ch.heigvd.iict.util.PoLUtils
import ch.heigvd.iict.web.admin.forms.BeaconFormData
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

/**
 * Represents the outcome of a form submission.
 * This allows for a clean way to return either a success (redirect) or failure (error page) response.
 */
sealed class FormProcessingResult {
    data class Success(val redirectResponse: Response) : FormProcessingResult()
    data class Failure(val errorResponse: Response) : FormProcessingResult()
}

/**
 * Processes form submissions from the beacon admin interface.
 *
 * @property beaconAdminService The service for beacon business logic.
 * @property viewRenderer The service for rendering error pages.
 */
@ApplicationScoped
class BeaconAdminFormHandler(
    private val beaconAdminService: BeaconAdminService,
    private val viewRenderer: BeaconAdminViewRenderer
) {

    /**
     * Processes the creation of a new beacon from form data.
     * @param formData The raw data submitted from the "Add Beacon" form.
     * @return A [FormProcessingResult] indicating success (redirect) or failure (error view).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun processCreateBeacon(formData: BeaconFormData): FormProcessingResult {
        try {
            val technicalId = formData.technicalId?.toIntOrNull()
                ?: throw IllegalArgumentException("Technical ID must be a valid number.")

            val name = formData.name.takeIf { !it.isNullOrBlank() }
                ?: throw IllegalArgumentException("Name is required.")

            val locationDescription = formData.locationDescription ?: ""

            val ed25519Bytes = validateHexKey(formData.publicKeyEd25519Hex, "Ed25519 Public Key")
            val x25519Bytes = formData.publicKeyX25519Hex?.let { validateHexKey(it, "X25519 Public Key") }

            beaconAdminService.addBeacon(technicalId, name, locationDescription, ed25519Bytes, x25519Bytes)

            val redirect = Response.seeOther(UriBuilder.fromPath("/admin/dashboard").build()).build()
            return FormProcessingResult.Success(redirect)

        } catch (e: Exception) {
            val errorResponse = viewRenderer.renderAddFormWithError(formData, e.message ?: "An unknown error occurred.")
            return FormProcessingResult.Failure(errorResponse)
        }
    }

    /**
     * Processes an update for an existing beacon from form data.
     * @param id The database ID of the beacon to update.
     * @param formData The raw data submitted from the "Edit Beacon" form.
     * @return A [FormProcessingResult] indicating success (redirect) or failure (error view).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun processUpdateBeacon(id: Long, formData: BeaconFormData): FormProcessingResult {
        val existingBeacon = beaconAdminService.findBeaconById(id)
            ?: return FormProcessingResult.Failure(
                viewRenderer.renderEditFormWithError(
                    null,
                    "Beacon not found with ID: $id"
                )
            )

        try {
            val name = formData.name.takeIf { !it.isNullOrBlank() }
                ?: throw IllegalArgumentException("Name is required.")

            val locationDescription = formData.locationDescription ?: ""
            beaconAdminService.updateBeacon(id, name, locationDescription)

            formData.publicKeyX25519Hex?.let {
                beaconAdminService.updateBeaconX25519Key(id, validateHexKey(it, "X25519 Public Key"))
            }

            val redirect = Response.seeOther(UriBuilder.fromPath("/admin/dashboard").build()).build()
            return FormProcessingResult.Success(redirect)

        } catch (e: Exception) {
            val errorResponse =
                viewRenderer.renderEditFormWithError(existingBeacon, e.message ?: "An unknown error occurred.")
            return FormProcessingResult.Failure(errorResponse)
        }
    }

    /**
     * Processes the deletion of a beacon.
     * @param id The database ID of the beacon to delete.
     * @return A [FormProcessingResult.Success] containing a redirect response.
     */
    fun processDeleteBeacon(id: Long): FormProcessingResult {
        beaconAdminService.deleteBeacon(id)
        val redirect = Response.seeOther(UriBuilder.fromPath("/admin/dashboard").build()).build()
        return FormProcessingResult.Success(redirect)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun validateHexKey(key: String?, keyName: String): ByteArray {
        if (key.isNullOrBlank() || key.length != 64) {
            throw IllegalArgumentException("$keyName must be 64 hex characters.")
        }
        if (!key.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            throw IllegalArgumentException("$keyName must be an hexadecimal value")
        }

        return PoLUtils.hexStringToUByteArray(key).asByteArray()
    }
}