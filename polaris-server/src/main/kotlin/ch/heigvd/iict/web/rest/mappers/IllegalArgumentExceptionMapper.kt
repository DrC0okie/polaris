package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * A JAX-RS `ExceptionMapper` that catches `IllegalArgumentException`.
 *
 * This mapper provides a standardized 400 Bad Request response for common
 * input validation errors thrown within the application logic.
 */
@Provider
class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {
    override fun toResponse(e: IllegalArgumentException) =
        Response.status(Response.Status.BAD_REQUEST)
            .entity(ErrorResponseDto(e.message!!))
            .build()
}