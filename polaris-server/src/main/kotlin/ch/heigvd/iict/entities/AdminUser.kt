package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.security.jpa.*
import jakarta.persistence.*

/**
 * JPA entity representing an administrative user for the web interface.
 * @note This entity is currently not used
 */
@Entity
@Table(name = "admin_users")
@SequenceGenerator(
    name = "admin_users_seq",
    sequenceName = "admin_users_seq",
    allocationSize = 50
)
class AdminUser : PanacheEntityBase {

    /** The unique identifier for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_users_seq")
    var id: Long? = null

    /** The user's login name, which must be unique. */
    @Username
    @Column(unique = true, nullable = false)
    lateinit var username: String

    /** The hashed password for the user. */
    @Password
    @Column(nullable = false)
    lateinit var passwordHash: String

    /** The role assigned to the user (e.g., "ADMIN", "USER"). */
    @Roles
    @Column(nullable = false)
    lateinit var role: String // "ADMIN", "USER"
}
