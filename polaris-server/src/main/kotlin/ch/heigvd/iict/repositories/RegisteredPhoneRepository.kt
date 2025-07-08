package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

/**
 * Panache repository for managing [RegisteredPhone] entities.
 */
@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class RegisteredPhoneRepository : PanacheRepository<RegisteredPhone> {

    /**
     * Finds a [RegisteredPhone] by its unique public key.
     * @param publicKey The Ed25519 public key of the phone.
     * @return The found [RegisteredPhone] or `null` if not found.
     */
    fun findByPublicKey(publicKey: ByteArray): RegisteredPhone? {
        return find("publicKey", publicKey).firstResult()
    }

    /**
     * Finds a [RegisteredPhone] by its unique API key.
     * This is used by the authentication filter to identify incoming requests.
     *
     * @param apiKey The API key provided in the request header.
     * @return The found [RegisteredPhone] or `null` if not found.
     */
    fun findByApiKey(apiKey: String): RegisteredPhone? {
        return find("apiKey", apiKey).firstResult()
    }
}