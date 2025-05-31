package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.security.jpa.*
import jakarta.persistence.*

@Entity
@Table(name = "admin_users")
class AdminUser : PanacheEntity() {

    @Username
    @Column(unique = true, nullable = false)
    lateinit var username: String

    @Password
    @Column(nullable = false)
    lateinit var passwordHash: String

    @Roles
    @Column(nullable = false)
    lateinit var role: String // "ADMIN", "USER"
}
