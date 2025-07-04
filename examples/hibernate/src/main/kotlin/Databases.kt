package com.example

import io.ktor.server.application.*
import jakarta.persistence.EntityManagerFactory
import org.hibernate.jpa.HibernatePersistenceConfiguration
import org.hibernate.tool.schema.Action
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
    return entityManagerFactory
}