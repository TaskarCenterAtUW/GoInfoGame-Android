package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class LongFormQuest(
    @SerialName("quest_answer_choices")
    val questAnswerChoices: List<QuestAnswerChoice?>? = null,
    @SerialName("quest_answer_dependency")
    @Serializable(with = QuestDependencySerializer::class)
    val questAnswerDependency: List<QuestAnswerDependency>? = null,
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
    val questType: String? = null,
    var visible: Boolean = true,
    var userInput: UserInput? = null,
    var selectedIndex: MutableList<Int>? = null,
) : Parcelable

@Parcelize
@Serializable
sealed class UserInput : Parcelable {
    data class Single(var answer: String? = null) : UserInput()
    data class Multiple(var answers: MutableList<String> = emptyList<String>().toMutableList()) :
        UserInput()

    fun isEmpty(): Boolean {
        return when (this) {
            is Single -> answer.isNullOrEmpty()
            is Multiple -> answers.isEmpty()
        }
    }
}
