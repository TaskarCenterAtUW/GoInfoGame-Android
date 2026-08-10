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
    /** The answer this question was pre-filled with from the element's existing tags when the
     *  form opened, or null if it was unanswered. Compared against [userInput] at submit time so
     *  only genuinely new/changed answers get resubmitted - see [UserInput.contentEquals]. */
    var seededAnswer: UserInput? = null,
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

/** Order-insensitive content comparison - [UserInput.Multiple.answers] can end up in a different
 *  order than how it was seeded (add/remove selection edits) even when the resulting set of
 *  answers is unchanged, so plain [equals] (list-order-sensitive) would false-positive as
 *  "changed". Used to decide which answers actually need resubmitting - see [LongFormQuest.seedFrom]. */
fun UserInput?.contentEquals(other: UserInput?): Boolean = when {
    this is UserInput.Single && other is UserInput.Single -> answer == other.answer
    this is UserInput.Multiple && other is UserInput.Multiple -> answers.toSet() == other.answers.toSet()
    this == null && other == null -> true
    else -> false
}

/** Pure evaluation of whether [this] question's questAnswerDependency conditions are satisfied,
 *  given a way to look up another question's currently-known answer value(s) by its questId. Used
 *  both for live in-form field visibility (LongFormAdapter, answers sourced from in-progress
 *  UserInput) and for tag-based completeness checks (AddGenericLong, answers sourced from
 *  element.tags) so the two can never disagree about what counts as "applicable". */
fun LongFormQuest.isVisibleGiven(
    questionExists: (questionId: Int) -> Boolean,
    answersOf: (questionId: Int) -> List<String>?,
): Boolean {
    for (dependency in questAnswerDependency ?: return true) {
        val required = dependency.requiredValue ?: continue
        val requiredQuestId = dependency.questionId ?: continue
        if (!questionExists(requiredQuestId)) continue
        val answers = answersOf(requiredQuestId)
        if (answers == null || answers.none { it in required }) return false
    }
    return true
}

/** Seeds [LongFormQuest.userInput]/[LongFormQuest.selectedIndex]/[LongFormQuest.seededAnswer] from
 *  the element's existing tag value for this question - so the form opens pre-filled with
 *  already-answered questions instead of always blank. Always fully resets these three fields
 *  (to blank when the tag is absent, not just when it's present): the [LongFormQuest] instances
 *  backing a given element type's question schema are shared/reused across every element of that
 *  type and every time its form is opened (they're the same objects passed via a fragment
 *  `arguments` Bundle, which - never leaving this process - hands back the same instances rather
 *  than fresh copies), so a conditional set-only-when-present would leak one element's (or an
 *  undone edit's stale pre-undo) answer into the next element/open that doesn't have that tag. */
fun LongFormQuest.seedFrom(tags: Map<String, String>) {
    val value = questTag?.let { tags[it] }
    if (value == null) {
        userInput = null
        seededAnswer = null
        selectedIndex = null
        return
    }
    val input = if (questType == "MultipleChoice") {
        UserInput.Multiple(value.split(";").toMutableList())
    } else {
        UserInput.Single(value)
    }
    userInput = input
    seededAnswer = input.snapshot()
    val answeredValues = when (input) {
        is UserInput.Single -> listOfNotNull(input.answer)
        is UserInput.Multiple -> input.answers
    }
    selectedIndex = questAnswerChoices
        ?.mapIndexedNotNull { index, choice -> index.takeIf { choice?.value in answeredValues } }
        ?.toMutableList()
        ?.takeIf { it.isNotEmpty() }
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
