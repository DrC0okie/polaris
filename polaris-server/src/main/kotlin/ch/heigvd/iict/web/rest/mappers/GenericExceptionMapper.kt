package ch.heigvd.iict.web.rest.mappers

import ch.heigvd.iict.dto.api.ErrorResponseDto
import io.quarkus.logging.Log
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

//@Provider
//class GenericExceptionMapper : ExceptionMapper<Exception> {
//    override fun toResponse(e: Exception): Response {
//        Log.error("Unhandled exception in JAX-RS layer", e)
//        return Response.status(INTERNAL_SERVER_ERROR)
//            .entity(ErrorResponseDto("Internal server error"))
//            .build()
//    }
//}