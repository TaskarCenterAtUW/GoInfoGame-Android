package de.westnordost.streetcomplete.quests.sidewalk_long_form.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quest(
    @SerialName("quest_answer_choices")
    val questAnswerChoices: List<QuestAnswerChoice?>? = null,
    @SerialName("quest_answer_dependency")
    val questAnswerDependency: QuestAnswerDependency? = null,
    @SerialName("quest_answer_validation")
    val questAnswerValidation: QuestAnswerValidation? = null,
    @SerialName("quest_description")
    val questDescription: String? = null,
    @SerialName("quest_id")
    val questId: Int? = null,
    @SerialName("quest_image_url")
    val questImageUrl: String? = null,
    @SerialName("quest_tag")
    val questTag: String? = null,
    @SerialName("quest_title")
    val questTitle: String? = null,
    @SerialName("quest_type")
    val questType: String? = null
)
