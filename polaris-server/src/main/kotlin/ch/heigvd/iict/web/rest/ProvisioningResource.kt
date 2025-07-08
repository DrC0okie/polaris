package ch.heigvd.iict.web.rest

import ch.heigvd.iict.dto.api.BeaconProvisioningListDto
import ch.heigvd.iict.services.api.ProvisioningApiService
import ch.heigvd.iict.web.rest.auth.Secured
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * JAX-RS resource for providing beacon data to authenticated mobile clients.
 * This endpoint is secured and requires a valid API key.
 *
 * @property provisioningService The service responsible for fetching and formatting beacon data.
 */
@Secured
@Path("/api/v1/beacons")
@ApplicationScoped
class ProvisioningResource {

    @Inject
    private lateinit var provisioningService: ProvisioningApiService

    /**
     * [GET] /api/v1/beacons
     * Returns the complete list of beacons known to the system. Mobile clients should
     * call this periodically to stay up-to-date.
     *
     * @return A [Response] containing the list of all provisioned beacons.
     */
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