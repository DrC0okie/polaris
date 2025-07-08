package ch.heigvd.iict.web

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * JAX-RS resource for the application's root endpoint.
 * Provides a simple landing page with information about the API.
 */
@Path("/")
@ApplicationScoped
class HomeResource {

    /**
     * Defines the type-safe Qute template for the home page.
     */
    @CheckedTemplate
    object Templates {
        /**
         * Renders the main landing page.
         * The `native fun` declaration tells Qute to look for a `home.html` file.
         */
        @JvmStatic
        external fun home(): TemplateInstance
    }

    /**
     * [GET] /
     * Serves the main landing page of the API.
     *
     * @return A Qute [TemplateInstance] that renders the `home.html` template.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun getHomePage(): TemplateInstance {
        return Templates.home()
    }
}