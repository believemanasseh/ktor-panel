package xyz.daimones.ktor.panel.database.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.kotlinx.BsonDecoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        value.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        encoder.encodeString(value.format(formatter))
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return when (decoder) {
            is BsonDecoder ->
                try {
                    // Try to decode as string first
                    LocalDateTime.parse(decoder.decodeString(), formatter)
                } catch (e: org.bson.BsonInvalidOperationException) {
                    // Fallback: decode as BSON DateTime
                    val bsonValue = decoder.decodeBsonValue()
                    val millis = bsonValue.asDateTime().value
                    val ldt = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDateTime()
                    LocalDateTime.parse(ldt.toString(), formatter)
                }

            else -> {
                LocalDateTime.parse(decoder.decodeString(), formatter)
            }
        }
    }
}