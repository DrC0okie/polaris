package ch.heigvd.iict.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.security.jpa.*
import jakarta.persistence.*

@Entity
@Table(name = "admin_users")
@SequenceGenerator(
    name = "admin_users_seq",
    sequenceName = "admin_users_seq",
    allocationSize = 50
)
class AdminUser : PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_users_seq")
    var id: Long? = null

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
