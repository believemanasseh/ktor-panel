package com.example

import io.ktor.server.application.*
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    val database = configureDatabases()
    // Configure and initialise admin interface library
    val configuration = Configuration(setAuthentication = true)
    val admin = Admin(this, configuration, database)
    admin.addView(EntityView(User::class))
}
