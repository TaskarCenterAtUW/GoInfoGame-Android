package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Parcelize
@Serializable
data class QuestAnswerDependency(
    @SerialName("question_id")
    val questionId: Int? = null,
    @SerialName("required_value")
    @Serializable(with = RequiredValueSerializer::class)
    val requiredValue: List<String>? = null
) : Parcelable


object RequiredValueSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RequiredValue", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder ?: throw IllegalStateException("This serializer can be used only with Json format")
        val element: JsonElement = input.decodeJsonElement()
        return when (element) {
            is JsonArray -> element.map { it.jsonPrimitive.content }
            is JsonPrimitive -> listOf(element.content)
            else -> throw IllegalStateException("Unexpected JSON element")
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        if (value.size == 1) {
            encoder.encodeString(value[0])
        } else {
            encoder.encodeSerializableValue(JsonArray.serializer(), JsonArray(value.map { JsonPrimitive(it) }))
        }
    }
}

object QuestDependencySerializer : KSerializer<List<QuestAnswerDependency>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("QuestAnswerDependencyList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<QuestAnswerDependency> {
        val input = decoder as? JsonDecoder ?: throw IllegalStateException("This serializer can be used only with Json format")
        val element: JsonElement = input.decodeJsonElement()
        return when (element) {
            is JsonArray -> element.map { input.json.decodeFromJsonElement(QuestAnswerDependency.serializer(), it) }
            is JsonObject -> listOf(input.json.decodeFromJsonElement(QuestAnswerDependency.serializer(), element))
            is JsonPrimitive -> listOf(input.json.decodeFromJsonElement(QuestAnswerDependency.serializer(), element))
            else -> throw IllegalStateException("Unexpected JSON element")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: List<QuestAnswerDependency>) {
        if (value.size == 1) {
            encoder.encodeSerializableValue(QuestAnswerDependency.serializer(), value[0])
        } else {
            encoder.encodeSerializableValue(JsonArray.serializer(), JsonArray(value.map {
                encoder.serializersModule.getContextual(QuestAnswerDependency::class)?.let { serializer ->
                    encoder.encodeSerializableValue(serializer, it)
                    JsonPrimitive("") // placeholder, not used
                } ?: throw IllegalStateException("No serializer found for QuestAnswerDependency")
            }))
        }
    }
}
