package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

fun Application.configureDatabases(): Database {
    val database = Database.connect(url = "jdbc:h2:mem:exposed_example;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    transaction(database) {
        SchemaUtils.create(Users)
        val hashedPassword = BCrypt.hashpw("password", BCrypt.gensalt())
        User.new {
            email = "test@email.com"
            firstName = "test"
            lastName = "user"
            password = hashedPassword
            created = LocalDateTime.now()
            modified = LocalDateTime.now()
        }
    }

    return database
}
