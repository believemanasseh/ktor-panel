package xyz.daimones.ktor.panel.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class AdminRole {
    SUPER_ADMIN,
    EDITOR,
    VIEWER
}

object AdminUsers : IntIdTable("admin_users") {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = enumerationByName("role", 15, AdminRole::class).default(AdminRole.SUPER_ADMIN)
    val created = datetime("created").default(LocalDateTime.now())
    val modified = datetime("modified").default(LocalDateTime.now())
}
