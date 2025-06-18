package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.MessageDelivery
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class MessageDeliveryRepository : PanacheRepository<MessageDelivery>