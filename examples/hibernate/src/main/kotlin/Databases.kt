package com.example

import io.ktor.server.application.*
import jakarta.persistence.EntityManagerFactory
import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.hibernate.tool.schema.Action
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.registerAdminEntity

fun Application.configureDatabases(): EntityManagerFactory {
    val config = HibernatePersistenceConfiguration("Users")
    registerAdminEntity(config) // Registers library's admin entity
    val entityManagerFactory =
        config
            .managedClass(User::class.java)
            .jdbcUrl("jdbc:h2:mem:hibernate_example;DB_CLOSE_DELAY=-1")
            .jdbcDriver("org.h2.Driver")
            .jdbcCredentials("sa", "")
            .schemaToolingAction(Action.CREATE_DROP)
            .jdbcPoolSize(16)
            .showSql(true, true, true)
            .createEntityManagerFactory()

    // Create an initial user entity
    val hashedPassword = BCrypt.hashpw("password", BCrypt.gensalt())
    val entity = User(
        email = "test@email.com",
        firstName = "test",
        lastName = "user",
        password = hashedPassword,
        image = "thumbnail data".toByteArray()
    )
    val entityManager = entityManagerFactory.createEntityManager()
    try {
        entityManager.transaction.begin()
        entityManager.merge(entity)
        entityManager.transaction.commit()
    } catch (e: Exception) {
        if (entityManager.transaction.isActive) {
            entityManager.transaction.rollback()
        }
        throw e
    } finally {
        entityManager.close()
    }

    return entityManagerFactory
}