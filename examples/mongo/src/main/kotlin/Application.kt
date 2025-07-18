package com.example

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import xyz.daimones.ktor.panel.Admin
import xyz.daimones.ktor.panel.Configuration
import xyz.daimones.ktor.panel.EntityView

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    var database: MongoDatabase? = null
    runBlocking(Dispatchers.IO) {
        val res = async {
            configureDatabase()
        }
        database = res.await()
        println("Database configured: ${database?.name}")
    }
    val configuration = Configuration(setAuthentication = false)
    val admin =
        Admin(this, configuration, database as MongoDatabase)
    admin.addView(EntityView(User::class))
}

