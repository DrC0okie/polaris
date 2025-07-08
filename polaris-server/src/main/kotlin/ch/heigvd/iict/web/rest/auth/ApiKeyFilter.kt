package ch.heigvd.iict.web.rest.auth

import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import ch.heigvd.iict.entities.RegisteredPhone
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.*
import jakarta.ws.rs.ext.Provider

/**
 * Filter that enforces API key-based authentication for endpoints annotated with `@Secured`.
 *
 * This filter runs before the target resource method is executed. It extracts the `x-api-key`
 * header, validates it against the database, and injects the corresponding [RegisteredPhone]
 * entity into the request context for later use by the resource.
 *
 * @property phoneRepo The repository used to look up phones by their API key.
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
class ApiKeyFilter @Inject constructor(
    private val phoneRepo: RegisteredPhoneRepository
) : ContainerRequestFilter {

    /**
     * Filters incoming requests, checking for a valid API key.
     * @param ctx The request context, used to access headers and properties.
     * @throws NotAuthorizedException if the API key is missing or invalid.
     */
    override fun filter(ctx: ContainerRequestContext) {
        val apiKey = ctx.getHeaderString("x-api-key")
            ?: throw NotAuthorizedException("Missing x-api-key header")
        val phone = phoneRepo.findByApiKey(apiKey)
            ?: throw NotAuthorizedException("Invalid API key")
        ctx.setProperty("authenticatedPhone", phone)
    }
}