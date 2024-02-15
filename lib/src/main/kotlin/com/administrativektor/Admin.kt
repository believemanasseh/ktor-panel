package com.administrativektor

import com.administrativektor.database.retrieveTableNames
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database


class Admin(private val application: Application, private val database: Database, val configuration: Configuration) {
    init {
        this.application.log.info("Admin class initialisation completed.")
    }

    private fun retrieveTableNames(): List<String> {
        return retrieveTableNames(database)
    }
}