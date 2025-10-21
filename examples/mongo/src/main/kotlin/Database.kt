package com.example

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod
import de.flapdoodle.embed.mongo.transitions.Mongod
import io.ktor.server.application.*
import org.mindrot.jbcrypt.BCrypt

suspend fun Application.configureDatabase(): MongoDatabase {
    val mongodConfig: ImmutableMongod = Mongod.instance()
    val version: Version.Main = Version.Main.V8_0

    val running = mongodConfig.start(version)
    val serverAddress: ServerAddress = running.current().serverAddress

    val uri = "mongodb://${serverAddress.host}:${serverAddress.port}"
    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase("user")

    // Create a collection and insert a sample user
    val collection = database.getCollection<User>("user")
    val hashedPassword = BCrypt.hashpw("password", BCrypt.gensalt())
    val user = User(
        email = "test@email.com",
        firstName = "test",
        lastName = "user",
        image = "/uploads/image.png",
        password = hashedPassword
    )
    val res = collection.insertOne(user)
    println("Inserted user with id: ${res.insertedId}")

    return database
}