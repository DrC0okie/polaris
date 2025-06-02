package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class RegisteredPhoneRepository : PanacheRepository<RegisteredPhone> {
    fun findByPhoneTechnicalId(technicalId: Long): RegisteredPhone? {
        return find("phoneTechnicalId", technicalId).firstResult()
    }

    fun findByPublicKey(publicKey: ByteArray): RegisteredPhone? {
        return find("publicKey", publicKey).firstResult()
    }

    fun findByApiKey(apiKey: String): RegisteredPhone? {
        return find("apiKey", apiKey).firstResult()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun findOrCreate(
        technicalId: Long,
        publicKey: ByteArray,
        userAgent: String?,
        now: Instant = Instant.now()
    ): RegisteredPhone {
        val phoneById = findByPhoneTechnicalId(technicalId)
        val phoneByPk = findByPublicKey(publicKey)

        when {
            // id and pk match -> update metadata
            phoneById != null && phoneByPk != null && phoneById.id == phoneByPk.id -> {
                phoneById.apply {
                    if (userAgent != null && this.userAgent != userAgent) {
                        this.userAgent = userAgent
                    }
                    this.lastSeenAt = now
                }
                return phoneById
            }

            // id exists but different pk -> reject
            phoneById != null && !phoneById.publicKey.contentEquals(publicKey) -> {
                throw IllegalStateException("Phone ID is already registered with another public key.")
            }

            // pk exists but different id -> reject
            phoneByPk != null && phoneByPk.phoneTechnicalId != technicalId -> {
                throw IllegalStateException("Public key is already registered with another phone ID.")
            }

            // pk exists but phoneById was null -> should not happen
            phoneByPk != null && phoneByPk.phoneTechnicalId == technicalId -> {
                Log.debug("Phone found by PK, ID matches. Updating info for ID $technicalId.")
                phoneByPk.apply {
                    if (userAgent != null && this.userAgent != userAgent) {
                        this.userAgent = userAgent
                    }
                    this.lastSeenAt = now
                }
                return phoneByPk
            }

            // Id and pk does not exist -> new phone to be registered
            phoneById == null && phoneByPk == null -> {
                Log.debug("Creating new RegisteredPhone for ID $technicalId.")
                return RegisteredPhone().apply {
                    this.phoneTechnicalId = technicalId
                    this.publicKey = publicKey
                    this.userAgent = userAgent
                    this.lastSeenAt = now
                }
            }

            // Should never hit this point
            else -> throw IllegalStateException("Unaccounted state. This should not happen.")
        }
    }
}