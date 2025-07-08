package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.dto.api.PoLTokenValidationResultDto
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.services.token.TokenProcessingService
import ch.heigvd.iict.web.rest.auth.Secured
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.*

/**
 * JAX-RS resource for submitting PoL tokens.
 * This endpoint is secured and requires a valid API key.
 *
 * @property processingService The service that orchestrates the validation and storage of tokens.
 */
@Secured
@Path("/api/v1/tokens")
@RequestScoped
class TokenResource @Inject constructor(
    private val processingService: TokenProcessingService
) {
    /** Injects the JAX-RS request context to access the authenticated phone identity. */
    @Context
    private lateinit var requestContext: ContainerRequestContext

    /**
     * [POST] /api/v1/tokens
     * Receives a PoL token from a mobile client, validates it, and persists it.
     *
     * @param dto The PoL token DTO submitted by the client.
     * @return A [Response] with status 201 Created on successful validation, or 422 Unprocessable Entity
     *         if the token is syntactically correct but fails validation (e.g., bad signature).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun submitPoLToken(@Valid dto: PoLTokenDto): Response {
        val phone = requestContext.getProperty("authenticatedPhone") as RegisteredPhone
        val result: PoLTokenValidationResultDto = processingService.process(dto, phone)
        return if (result.isValid) {
            Response.status(Response.Status.CREATED).entity(result).build()
        } else {
            Response.status(422).entity(result).build()
        }
    }
}