package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.ErrorResponseDto
import ch.heigvd.iict.dto.api.PhoneRegistrationRequestDto
import ch.heigvd.iict.services.api.PhoneRegistrationService
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/v1/register")
@ApplicationScoped
class PhoneRegistrationResource {

    @Inject
    private lateinit var phoneRegistrationService: PhoneRegistrationService

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerPhone(@Valid requestDto: PhoneRegistrationRequestDto): Response {
        Log.info("POST /api/v1/register - Attempting to register phone with id: ${requestDto.phoneTechnicalId}")
        // TODO: Vérifier une clé d'API globale pour l'enregistrement

        return try {
            val result = phoneRegistrationService.registerPhoneAndGetBeacons(requestDto)
            if (result.assignedPhoneId != null) {
                Response.status(Response.Status.CREATED).entity(result).build()
            } else {
                Response.status(Response.Status.BAD_REQUEST).entity(result).build()
            }
        } catch (e: PhoneRegistrationService.RegistrationConflictException) {
            Log.warn("Phone registration conflict: ${e.message}")
            Response.status(Response.Status.CONFLICT).entity(ErrorResponseDto(e.message ?: "Registration conflict occurred.")).build()
        } catch (e: IllegalArgumentException) {
            Log.warn("Invalid registration request: ${e.message}")
            Response.status(Response.Status.BAD_REQUEST).entity(ErrorResponseDto(e.message ?: "Invalid registration request.")).build()
        } catch (e: Exception) {
            Log.error("Error during phone registration: ${e.message}", e)
            Response.serverError().entity("An internal error occurred during registration.").build()
        }
    }
}