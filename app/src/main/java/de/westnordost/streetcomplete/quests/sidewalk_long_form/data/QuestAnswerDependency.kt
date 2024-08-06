package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestAnswerDependency(
    @SerialName("question_id")
    val questionId: Int? = null,
    @SerialName("required_value")
    val requiredValue: List<String>? = null
)
