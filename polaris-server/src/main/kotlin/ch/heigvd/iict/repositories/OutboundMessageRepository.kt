package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.OutboundMessage
import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType

@ApplicationScoped
class OutboundMessageRepository(
    private val entityManager: EntityManager
) : PanacheRepository<OutboundMessage> {

    fun findAndClaimAvailableJob(phone: RegisteredPhone, maxJobs: Int = 1): OutboundMessage? {
        // The query remains the same.
        val query = """
            SELECT m FROM OutboundMessage m 
            WHERE (m.status = 'PENDING' OR m.status = 'DELIVERING') 
            AND m.deliveryCount < m.redundancyFactor
            AND NOT EXISTS (
                SELECT 1 FROM MessageDelivery d 
                WHERE d.outboundMessage = m AND d.phone = :phone
            )
            ORDER BY m.createdAt ASC
        """

        val availableMessages = entityManager.createQuery(query, OutboundMessage::class.java)
            .setParameter("phone", phone)
            .setMaxResults(maxJobs)
            .resultList

        if (availableMessages.isEmpty()) {
            return null
        }

        val messageToClaim = availableMessages.first()

        // Acquire the lock on this specific entity instance
        entityManager.lock(messageToClaim, LockModeType.PESSIMISTIC_WRITE)

        // Double-check condition post-lock
        if (messageToClaim.deliveryCount >= messageToClaim.redundancyFactor) {
            // Someone else claimed it.
            return null
        }

        // Return the locked entity.
        return messageToClaim
    }

    fun getNextServerMsgId(): Long {
        return entityManager
            .createNativeQuery("SELECT nextval('server_msg_id_seq')")
            .singleResult as Long
    }

}