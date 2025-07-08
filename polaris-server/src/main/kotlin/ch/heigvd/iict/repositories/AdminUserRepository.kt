package ch.heigvd.iict.repositories

import ch.heigvd.iict.entities.AdminUser
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

/**
 * Panache repository for managing [AdminUser] entities.
 * Provides custom queries for admin user management.
 */
@ApplicationScoped
class AdminUserRepository : PanacheRepository<AdminUser> {

    /**
     * Finds an [AdminUser] by their unique username.
     * @param username The username to search for.
     * @return The found [AdminUser] or `null` if no user exists with that username.
     */
    fun findByUsername(username: String): AdminUser? {
        return find("username", username).firstResult()
    }

    /**
     * Creates and persists a new [AdminUser].
     * note: This method expects a pre-hashed password.
     *
     * @param username The username for the new user. Must be unique.
     * @param rawPasswordNotHashed The password that has already been hashed.
     * @param role The role to assign to the new user.
     * @return The newly created and persisted [AdminUser].
     * @throws IllegalArgumentException if a user with the given username already exists.
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