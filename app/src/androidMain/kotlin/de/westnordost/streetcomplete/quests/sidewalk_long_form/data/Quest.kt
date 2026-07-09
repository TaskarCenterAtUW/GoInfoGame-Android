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

/** A frozen copy of this quest's current state, safe to keep around and compare against later -
 *  [LongFormQuest.selectedIndex] and [UserInput.Multiple.answers] are mutated in place elsewhere
 *  (`.add()`/`.remove()`), so a shallow `.copy()` alone would still share those mutable lists with
 *  the live, later-mutated instance. Used to give DiffUtil genuinely independent before/after
 *  values to compare (see LongFormAdapter). */
fun LongFormQuest.snapshot(): LongFormQuest = copy(
    selectedIndex = selectedIndex?.toMutableList(),
    userInput = userInput?.snapshot()
)

fun UserInput.snapshot(): UserInput = when (this) {
    is UserInput.Single -> copy()
    is UserInput.Multiple -> copy(answers = answers.toMutableList())
}
