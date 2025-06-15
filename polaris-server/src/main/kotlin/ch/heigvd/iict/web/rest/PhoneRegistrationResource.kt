package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.PhoneRegistrationRequestDto
import ch.heigvd.iict.services.api.PhoneRegistrationService
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status.BAD_REQUEST
import jakarta.ws.rs.core.Response.Status.CREATED

@Path("/api/v1/register")
@RequestScoped
class PhoneRegistrationResource @Inject constructor(
    private val phoneRegistrationService: PhoneRegistrationService
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerPhone(@Valid dto: PhoneRegistrationRequestDto): Response {
        val result = phoneRegistrationService.register(dto)
        return when {
            true -> Response.status(CREATED).entity(result).build()
            else -> Response.status(BAD_REQUEST).entity(result).build()
        }
    }
}