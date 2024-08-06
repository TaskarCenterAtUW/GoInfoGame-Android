package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestAnswerChoice(
    @SerialName("choice_follow_up")
    val choiceFollowUp: String? = null,
    @SerialName("choice_text")
    val choiceText: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("value")
    val value: String? = null
)
