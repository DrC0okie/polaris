package ch.heigvd.iict.web.rest.auth

import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.*
import jakarta.ws.rs.ext.Provider

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
class ApiKeyFilter @Inject constructor(
    private val phoneRepo: RegisteredPhoneRepository
) : ContainerRequestFilter {
    override fun filter(ctx: ContainerRequestContext) {
        val apiKey = ctx.getHeaderString("x-api-key")
            ?: throw NotAuthorizedException("Missing x-api-key header")
        val phone = phoneRepo.findByApiKey(apiKey)
            ?: throw NotAuthorizedException("Invalid API key")
        ctx.setProperty("authenticatedPhone", phone)
    }
}