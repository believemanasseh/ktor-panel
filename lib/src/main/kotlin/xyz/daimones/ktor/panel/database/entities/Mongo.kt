package xyz.daimones.ktor.panel.database.entities


import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import xyz.daimones.ktor.panel.database.serialization.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
internal data class MongoAdminUser(
    @SerialName("_id")
    @Contextual
    val id: ObjectId,
    val username: String,
    val password: String,
    val role: AdminRole = AdminRole.SUPER_ADMIN,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val modified: LocalDateTime = LocalDateTime.now()
)