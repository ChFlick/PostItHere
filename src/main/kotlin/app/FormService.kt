package app

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val SUBMITTED_FORMS_COLLECTION = "forms"

@ExperimentalSerializationApi
@Serializer(forClass = ZonedDateTime::class)
object InstantSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value))
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(decoder.decodeString()))
    }
}

@ExperimentalSerializationApi
@Serializable
data class FormSubmit(
    val formId: String,
    val origin: String?,
    val parameters: Map<String, String>,
    @Serializable(with = InstantSerializer::class)
    val datetime: ZonedDateTime
)

class FormService(database: CoroutineDatabase) {
    @ExperimentalSerializationApi
    private val requestCollection = database.getCollection<FormSubmit>(SUBMITTED_FORMS_COLLECTION)

    @ExperimentalSerializationApi
    suspend fun getSubmitsByFormId(formId: String): List<FormSubmit> {
        return requestCollection.find(FormSubmit::formId eq formId).toList()
    }

    @ExperimentalSerializationApi
    suspend fun insertRequest(form: FormSubmit): Boolean {
        return requestCollection.insertOne(form).wasAcknowledged()
    }
}