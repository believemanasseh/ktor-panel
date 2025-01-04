package com.administrativektor

import com.administrativektor.database.retrieveTableNames
import io.ktor.server.application.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database


class Admin(private val application: Application, private val database: Database, val configuration: Configuration) {
    private val modelViews: MutableList<BaseView> = mutableListOf()
    private val models: MutableList<IntIdTable> = mutableListOf()

    init {
        this.application.log.info("Admin class initialisation completed.")
    }

    private fun returnViews(): MutableList<BaseView> {
        return this.modelViews
    }

    private fun retrieveTableNames(): List<String> {
        return retrieveTableNames(database)
    }

    fun addView(view: BaseView): Boolean {
        this.models.add(view.model)
        return this.modelViews.add(view)
    }

    fun addViews(views: MutableList<BaseView>) {
        for (view in views) {
            this.models.add(view.model)
            this.modelViews.add(view)
        }
    }

    fun registerRoutes() {
        for (view in this.modelViews) {
            view.renderIndexView("index.hbs", mapOf("models" to this.models, "adminName" to configuration.adminName))
            view.renderCreateView("create.hbs", mapOf("model" to view.model))
            view.renderDetailsView("details.hbs", mapOf("model" to view.model))
        }
    }
}