package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import kotlinx.serialization.SerializationException

/**
 * A JAX-RS `ExceptionMapper` that catches `SerializationException` from `kotlinx.serialization`.
 *
 * This mapper handles errors that occur when a client sends a malformed JSON request body,
 * returning a clear 400 Bad Request response instead of a generic 500 server error.
 */
@Provider
class SerializationExceptionMapper : ExceptionMapper<SerializationException> {
    override fun toResponse(exception: SerializationException): Response {
        val message = "Malformed JSON request body: ${exception.message?.substringBefore(" at path")}"
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(ErrorResponseDto(message))
            .build()
    }
}