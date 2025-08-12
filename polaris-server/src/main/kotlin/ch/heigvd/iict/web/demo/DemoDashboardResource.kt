package ch.heigvd.iict.web.demo

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import java.net.URI

@Path("/demo/dashboard")
class DemoDashboardResource {

    @GET
    fun getDashboard(): Response {
        // Redirection HTTP vers le fichier statique
        return Response.temporaryRedirect(URI.create("/dashboard.html")).build()
    }
}
