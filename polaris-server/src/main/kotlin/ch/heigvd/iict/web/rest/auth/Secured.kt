package ch.heigvd.iict.web.rest.auth

import jakarta.ws.rs.NameBinding

/**
 * A JAX-RS name-binding annotation used to mark resource classes or methods
 * that require API key authentication.
 *
 * The [ApiKeyFilter] is automatically triggered for any endpoint annotated with `@Secured`.
 */
@NameBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Secured