package ch.heigvd.iict

import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
class ExampleResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "Hello from POLARIS Server"
}

@Path("/token")
class TokenResource {
    @Inject
    lateinit var verificationService: SignatureVerificationService


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun receiveToken(token: PoLToken): Response {
        println("Received PoLToken: $token")

        val isValid = verificationService.verifyToken(token)

        return if (isValid) {
            println("PoLToken successfully validated.")
            // TODO: Store the token, update database, trigger further actions, etc.
            Response.ok(GenericResponse("Success", "Token validated")).build()
        } else {
            println("PoLToken validation failed.")
            Response.status(Response.Status.BAD_REQUEST)
                .entity(GenericResponse("Failure", "Token validation failed")).build()
        }
    }
}