package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestAnswerValidation(
    @SerialName("min")
    val min: Int? = null
)
