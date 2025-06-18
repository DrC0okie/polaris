package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class RegisteredPhoneRepository : PanacheRepository<RegisteredPhone> {
    fun findByPublicKey(publicKey: ByteArray): RegisteredPhone? {
        return find("publicKey", publicKey).firstResult()
    }

    fun findByApiKey(apiKey: String): RegisteredPhone? {
        return find("apiKey", apiKey).firstResult()
    }
}