package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

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
