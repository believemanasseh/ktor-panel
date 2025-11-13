package com.example

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import xyz.daimones.ktor.panel.database.EntityName
import xyz.daimones.ktor.panel.database.FileUploadField
import xyz.daimones.ktor.panel.database.UpdateField
import xyz.daimones.ktor.panel.database.serialization.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class Role { SUPER_ADMIN, EDITOR, VIEWER }

@EntityName("users")
data class User(
    @BsonId
    val id: ObjectId? = null,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val password: String,
    val isActive: Boolean = false,
    val role: Role = Role.SUPER_ADMIN,
    @FileUploadField(storage = "local", path = "/uploads")
    val image: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime = LocalDateTime.now(),
    @UpdateField
    @Serializable(with = LocalDateTimeSerializer::class)
    val modified: LocalDateTime = LocalDateTime.now()
)

@EntityName("truths")
@Serializable
data class Truth(
    @SerialName("_id")
    @Contextual val id: ObjectId? = null,
    val email: String,
    val value: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime = LocalDateTime.now(),
    @UpdateField
    @Serializable(with = LocalDateTimeSerializer::class)
    val modified: LocalDateTime = LocalDateTime.now()
)