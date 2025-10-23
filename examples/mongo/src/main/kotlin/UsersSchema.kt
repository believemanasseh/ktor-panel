package com.example

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import xyz.daimones.ktor.panel.database.FileUpload
import xyz.daimones.ktor.panel.database.serialization.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class Role { SUPER_ADMIN, EDITOR, VIEWER }

@Serializable
data class User(
    @SerialName("_id")
    @Contextual val id: ObjectId? = null,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val password: String,
    val isActive: Boolean = false,
    val role: Role = Role.SUPER_ADMIN,
    @FileUpload(storage = "local", path = "/uploads")
    val image: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val modified: LocalDateTime = LocalDateTime.now()
)