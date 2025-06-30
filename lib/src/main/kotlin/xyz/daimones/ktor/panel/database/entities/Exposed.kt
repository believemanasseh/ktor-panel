package xyz.daimones.ktor.panel.database.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime


object AdminUsers : IntIdTable("admin_users") {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = enumerationByName("role", 15, AdminRole::class).default(AdminRole.SUPER_ADMIN)
    val created = datetime("created").default(LocalDateTime.now())
    val modified = datetime("modified").default(LocalDateTime.now())
}

class AdminUser(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AdminUser>(AdminUsers)

    var username by AdminUsers.username
    var password by AdminUsers.password
    var role by AdminUsers.role
    var created by AdminUsers.created
    var modified by AdminUsers.modified
}