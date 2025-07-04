package com.example

import io.ktor.server.application.*
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.ModelView

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()
    val entityManagerFactory = configureDatabases()
    // Configure and initialise admin interface library
    val configuration = Configuration(setAuthentication = true)
    val admin = Admin(this, configuration, entityManagerFactory = entityManagerFactory)
    admin.addView(ModelView(User()))
}
