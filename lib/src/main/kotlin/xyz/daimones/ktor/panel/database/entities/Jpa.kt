package xyz.daimones.ktor.panel.database.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admin_users")
class JpaAdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "username", length = 255, unique = true, nullable = false)
    var username: String,

    @Column(name = "password", length = 255, nullable = false)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 15, nullable = false)
    var role: AdminRole = AdminRole.SUPER_ADMIN,

    @Column(name = "created", nullable = false, updatable = false)
    var created: LocalDateTime = LocalDateTime.now(),

    @Column(name = "modified", nullable = false)
    var modified: LocalDateTime = LocalDateTime.now()
)