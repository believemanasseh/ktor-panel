package xyz.daimones.ktor.panel.database.entities


import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.LocalDateTime

@Serializable
data class MongoAdminUser(
    @SerialName("_id")
    @Contextual
    val id: ObjectId,
    val username: String,
    val password: String,
    val role: AdminRole = AdminRole.SUPER_ADMIN,
    val created: String = LocalDateTime.now().toString(),
    val modified: String = LocalDateTime.now().toString()
)