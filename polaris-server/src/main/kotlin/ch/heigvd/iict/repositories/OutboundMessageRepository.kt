package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.OutboundMessage
import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType

/**
 * Panache repository for managing [OutboundMessage] entities.
 * Implements logic for finding and claiming message jobs in a concurrent environment.
 *
 * @property entityManager The JPA EntityManager, injected for pessimistic locking.
 */
@ApplicationScoped
class OutboundMessageRepository(
    private val entityManager: EntityManager
) : PanacheRepository<OutboundMessage> {

    /**
     * Finds and claims available outbound message jobs for a specific phone
     *
     * @param phone The [RegisteredPhone] requesting a job.
     * @param maxJobs The maximum number of jobs to claim in this transaction.
     * @return A list of [OutboundMessage]s that have been successfully claimed by this transaction.
     */
    fun findAndClaimAvailableJob(phone: RegisteredPhone, maxJobs: Int = 1): List<OutboundMessage> {
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

        // Find potential candidates.
        val availableMessages = entityManager.createQuery(query, OutboundMessage::class.java)
            .setParameter("phone", phone)
            .setMaxResults(maxJobs)
            .resultList

        val claimedMessages = mutableListOf<OutboundMessage>()

        // Loop through candidates and try to claim them one by one.
        for (messageToClaim in availableMessages) {
            try {
                // Acquire lock. If another transaction has a lock, this will wait or timeout.
                entityManager.lock(messageToClaim, LockModeType.PESSIMISTIC_WRITE)

                if (messageToClaim.deliveryCount < messageToClaim.redundancyFactor) {
                    // The job is still available. Add it to the list.
                    claimedMessages.add(messageToClaim)
                }
            } catch (e: Exception) {
                Log.warn("Failed to acquire lock on message ${messageToClaim.id}", e)
            }
        }

        return claimedMessages
    }

    /**
     * Retrieves the next unique, sequential ID for a server-originated message.
     * @return The next available server message ID.
     */
    fun getNextServerMsgId(): Long {
        return entityManager
            .createNativeQuery("SELECT nextval('server_msg_id_seq')")
            .singleResult as Long
    }
}