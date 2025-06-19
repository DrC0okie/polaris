package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.ServerInfoDto
import ch.heigvd.iict.services.crypto.KeyManager
import ch.heigvd.iict.util.PoLUtils.toHexString
import ch.heigvd.iict.web.rest.auth.Secured
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Secured
@Path("/api/v1/server-info")
@ApplicationScoped
class ServerInfoResource(private val keyManager: KeyManager) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @OptIn(ExperimentalUnsignedTypes::class)
    fun getServerInfo(): Response {
        val dto = ServerInfoDto(
            serverX25519PublicKey = keyManager.serverPublicKey.toHexString()
        )
        return Response.ok(dto).build()
    }
}