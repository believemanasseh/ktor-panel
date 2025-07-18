package com.example

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    val entityManagerFactory = configureDatabases()
    // Configure and initialise admin interface library
    val configuration = Configuration(setAuthentication = true)
    val admin = Admin(this, configuration, entityManagerFactory = entityManagerFactory)
    admin.addView(EntityView(User::class))
}

