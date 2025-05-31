package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.AdminUser
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional


@ApplicationScoped
class AdminUserRepository : PanacheRepository<AdminUser>{

    fun findByUsername(username: String): AdminUser? {
        return find("username", username).firstResult()
    }

    /**
     * Creates a new admin user
     */
    @Transactional
    fun createAdmin(username: String, rawPasswordNotHashed: String, role: String): AdminUser {
        if (findByUsername(username) != null) {
            throw IllegalArgumentException("Username '$username' already exists.")
        }
        val admin = AdminUser().apply {
            this.username = username
            this.passwordHash = rawPasswordNotHashed
            this.role = role
        }
        persist(admin)
        return admin
    }
}