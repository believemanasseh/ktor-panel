package com.example

import io.ktor.server.application.*
import jakarta.persistence.EntityManagerFactory
import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.hibernate.tool.schema.Action
import org.mindrot.jbcrypt.BCrypt
import xyz.daimones.ktor.panel.database.entities.JpaAdminUser


fun Application.configureDatabases(): EntityManagerFactory {
    val entityManagerFactory =
        HibernatePersistenceConfiguration("Users")
            .managedClass(User::class.java)
            .managedClass(JpaAdminUser::class.java)
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
        password = hashedPassword
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