package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.services.api.TokenApiService
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/v1/tokens")
@ApplicationScoped
class TokenResource {

    @Inject
    private lateinit var tokenApiService: TokenApiService

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submitPoLToken(
        @Valid tokenDto: PoLTokenDto,
        @HeaderParam("User-Agent") userAgent: String?
    ): Response {
        // TODO: later implement API keys
        // val apiKey = headers.getHeaderString("X-API-Key")
        // if (!isValidApiKey(apiKey)) {
        //     return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or missing API Key.").build()
        // }

        return try {
            val result = tokenApiService.processAndValidatePoLToken(tokenDto, userAgent)
            if (result.isValid) {
                Response.status(Response.Status.CREATED).entity(result).build()
            } else {
                Log.warn("PoLToken processed but is INVALID. Reason: ${result.message}")
                val unprocessableContent = 422
                Response.status(unprocessableContent).entity(result).build()
            }
        } catch (e: IllegalArgumentException) {
            Log.warn("Invalid PoLToken submission: ${e.message}")
            Response.status(Response.Status.BAD_REQUEST).entity(mapOf("error" to e.message)).build()
        }
        catch (e: Exception) {
            Log.error("Error processing PoLToken", e)
            Response.serverError().entity("An internal error occurred while processing the token.").build()
        }
    }
}