package com.example

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class Role { SUPER_ADMIN, EDITOR, VIEWER }

object Users : IntIdTable() {
    val email: Column<String> = varchar("email", length = 50).uniqueIndex()
    val firstName: Column<String> = varchar("first_name", length = 255)
    val lastName: Column<String> = varchar("last_name", length = 255)
    val password: Column<String> = varchar("password", length = 100)
    val role = enumerationByName("role", 15, Role::class).default(Role.SUPER_ADMIN)
    val isActive: Column<Boolean> = bool("is_active").default(false)
    val created: Column<LocalDateTime> = datetime("created").default(LocalDateTime.now())
    val modified: Column<LocalDateTime> = datetime("modified").default(LocalDateTime.now())
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var email by Users.email
    var firstName by Users.firstName
    var lastName by Users.lastName
    var password by Users.password
    var isActive by Users.isActive
    val role by Users.role
    var created by Users.created
    var modified by Users.modified
}
