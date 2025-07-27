package com.example

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
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
    val created: String = LocalDateTime.now().toString(),
    val modified: String = LocalDateTime.now().toString()
)