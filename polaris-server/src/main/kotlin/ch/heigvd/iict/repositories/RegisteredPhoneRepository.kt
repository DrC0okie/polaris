package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class RegisteredPhoneRepository : PanacheRepository<RegisteredPhone>  {
    fun findByPhoneTechnicalId(technicalId: Long): RegisteredPhone? {
        return find("phoneTechnicalId", technicalId).firstResult()
    }

    fun findByPublicKey(publicKey: ByteArray): RegisteredPhone? {
        return find("publicKey", publicKey).firstResult()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun findOrCreate(id: Long, publicKey: ByteArray, userAgent: String?, now: Instant = Instant.now() ): RegisteredPhone {
        var phone = findByPhoneTechnicalId(id)
        if (phone == null) {
            val phoneByPk = findByPublicKey(publicKey)
            if (phoneByPk != null) {
                if (phoneByPk.phoneTechnicalId == id) {
                    phone = phoneByPk
                } else {
                    throw IllegalStateException(
                        "Public key already registered with a different phoneTechnicalId. " +
                                "Existing ID: ${phoneByPk.phoneTechnicalId}, New ID: $id"
                    )
                }
            }
        }

        if (phone == null) {
            phone = RegisteredPhone().apply {
                this.phoneTechnicalId = id
                this.publicKey = publicKey
                this.userAgent = userAgent
            }
            persist(phone)
            phone.lastSeenAt = now
        } else {
            var modified = false
            if (userAgent != null && phone.userAgent != userAgent) {
                phone.userAgent = userAgent
                modified = true
            }
            if (phone.lastSeenAt == null || now.isAfter(phone.lastSeenAt)) {
                phone.lastSeenAt = now
                modified = true
            }
        }
        return phone!!
    }
}