package ch.heigvd.iict

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/")
class ExampleResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "Hello from POLARIS Server"
}