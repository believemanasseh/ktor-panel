package com.administrativektor

import com.administrativektor.database.retrieveTableNames
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database


class Admin(private val application: Application, private val database: Database, val configuration: Configuration) {
    private val views: MutableList<View> = mutableListOf()

    init {
        this.application.log.info("Admin class initialisation completed.")
    }

    fun returnViews(): MutableList<View> {
        return this.views
    }

    private fun retrieveTableNames(): List<String> {
        return retrieveTableNames(database)
    }

    fun addView(view: View): Boolean {
        return this.views.add(view)
    }

    fun addViews(views: MutableList<View>) {
        for (view in views) {
            this.views.add(view)
        }
    }
}