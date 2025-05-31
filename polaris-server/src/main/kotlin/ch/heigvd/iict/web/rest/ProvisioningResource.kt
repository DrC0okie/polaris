package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.BeaconProvisioningListDto
import ch.heigvd.iict.services.api.ProvisioningApiService
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/v1/beacons")
@ApplicationScoped
class ProvisioningResource {

    @Inject
    private lateinit var provisioningService: ProvisioningApiService

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getBeaconsForProvisioning(): Response {
        return try {
            val beaconsDtoList = provisioningService.getBeaconsForProvisioning()
            val responseWrapper = BeaconProvisioningListDto(beaconsDtoList)
            Response.ok(responseWrapper).build()
        } catch (e: Exception) {
            Log.error("Error in getBeaconsForProvisioning resource: ${e.message}", e)
            Response.serverError().entity("An internal error occurred while fetching beacon data.").build()
        }
    }
}