package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import ch.heigvd.iict.web.rest.auth.NotAuthorizedException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * A JAX-RS `ExceptionMapper` that catches the custom [NotAuthorizedException].
 *
 * This mapper transforms authentication or authorization failures into a
 * standardized 401 Unauthorized HTTP response.
 */
@Provider
class NotAuthorizedExceptionMapper
    : ExceptionMapper<NotAuthorizedException> {
    override fun toResponse(e: NotAuthorizedException) =
        Response.status(Response.Status.UNAUTHORIZED)
            .entity(ErrorResponseDto(e.message!!))
            .build()
}