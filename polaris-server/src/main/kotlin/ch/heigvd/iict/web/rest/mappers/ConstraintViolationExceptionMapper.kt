package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * A JAX-RS `ExceptionMapper` that catches `ConstraintViolationException`.
 *
 * This mapper intercepts exceptions thrown by Jakarta Bean Validation (e.g., from `@Valid` on a DTO)
 * and transforms them into a user-friendly 400 Bad Request HTTP response with a detailed error message.
 */
@Provider
class ConstraintViolationExceptionMapper
    : ExceptionMapper<ConstraintViolationException> {
    override fun toResponse(e: ConstraintViolationException): Response {
        val messages = e.constraintViolations
            .joinToString("; ") { "${it.propertyPath}: ${it.message}" }
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ErrorResponseDto(messages))
            .build()
    }
}
