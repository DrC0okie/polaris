package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.MessageDelivery
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

/**
 * Panache repository for managing [MessageDelivery] entities.
 * This repository uses the default Panache methods without custom queries.
 */
@ApplicationScoped
class MessageDeliveryRepository : PanacheRepository<MessageDelivery>