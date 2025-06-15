package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import com.fasterxml.jackson.core.JsonParseException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class JsonParseExceptionMapper
    : ExceptionMapper<JsonParseException> {
    override fun toResponse(e: JsonParseException) =
        Response.status(Response.Status.BAD_REQUEST)
            .entity(ErrorResponseDto("Malformed JSON: ${e.originalMessage}"))
            .build()
}