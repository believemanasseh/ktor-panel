package com.example

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod
import de.flapdoodle.embed.mongo.transitions.Mongod
import io.ktor.server.application.*

fun Application.configureDatabase(): MongoDatabase {
    val mongodConfig: ImmutableMongod = Mongod.instance()
    val version: Version.Main = Version.Main.V8_0

    val running = mongodConfig.start(version)
    val serverAddress: ServerAddress = running.current().serverAddress

    val uri = "mongodb://${serverAddress.host}:${serverAddress.port}"
    val mongoClient = MongoClient.create(uri)
    return mongoClient.getDatabase("test")
}