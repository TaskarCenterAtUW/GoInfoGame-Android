package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.text.Editable
import androidx.recyclerview.widget.RecyclerView
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerChoice
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerDependency
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerValidation
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.seedFrom
import de.westnordost.streetcomplete.testutils.on
import org.mockito.Mockito

/* Question ids/tags mirror WorkspaceViewModel.DEFAULT_TEST_LONG_FORM_JSON's "Sidewalks" element,
 * which covers every question type plus both a dependency and a photo follow-up. */
const val SURFACE = 101
const val SURFACE_DESCRIPTION = 102
const val WIDTH = 103
const val OBSTRUCTION = 104
const val OBSTRUCTION_TYPE = 105

const val PHOTO_FOLLOW_UP = "Please take a photo of the obstruction."

fun choice(value: String, followUp: String? = null) =
    QuestAnswerChoice(value = value, choiceText = value, choiceFollowUp = followUp)

fun dependsOn(questId: Int, vararg values: String) =
    listOf(QuestAnswerDependency(questionId = questId, requiredValue = values.toList()))

/** Fresh instances every call - LongFormQuest is mutated in place by the form, so tests must
 *  never share them. */
fun sidewalkQuests(): List<LongFormQuest> = listOf(
    LongFormQuest(
        questId = SURFACE, questTag = "ext:surface", questType = "ExclusiveChoice",
        questAnswerChoices = listOf(choice("asphalt"), choice("concrete"), choice("other")),
    ),
    LongFormQuest(
        questId = SURFACE_DESCRIPTION, questTag = "ext:surface:description", questType = "TextEntry",
        questAnswerDependency = dependsOn(SURFACE, "other"),
    ),
    LongFormQuest(
        questId = WIDTH, questTag = "width", questType = "Numeric",
        questAnswerValidation = QuestAnswerValidation(min = 12, max = 240),
    ),
    LongFormQuest(
        questId = OBSTRUCTION, questTag = "ext:obstruction", questType = "ExclusiveChoice",
        questAnswerChoices = listOf(choice("yes"), choice("no")),
    ),
    LongFormQuest(
        questId = OBSTRUCTION_TYPE, questTag = "ext:obstruction:type", questType = "MultipleChoice",
        questAnswerDependency = dependsOn(OBSTRUCTION, "yes"),
        questAnswerChoices = listOf(choice("bollard"), choice("pole"), choice("other", PHOTO_FOLLOW_UP)),
    ),
)

/** Every sidewalk question answered, with the obstruction photo follow-up NOT selected. */
val FULLY_ANSWERED_TAGS = mapOf(
    "ext:surface" to "other",
    "ext:surface:description" to "cobbles",
    "width" to "60",
    "ext:obstruction" to "yes",
    "ext:obstruction:type" to "bollard;pole",
)

/** A LongFormAdapter usable on the plain JVM: RecyclerView.Adapter's observable is backed by the
 *  stubbed android.database.Observable, whose observer list is never initialized, so every
 *  notify*() (DiffUtil dispatch, photo refresh) would NPE without this. */
fun jvmLongFormAdapter(): LongFormAdapter<List<LongFormQuest?>> {
    val adapter = LongFormAdapter<List<LongFormQuest?>>({}, {}, {})
    val observable = RecyclerView.Adapter::class.java.getDeclaredField("mObservable")
        .apply { isAccessible = true }
        .get(adapter)
    android.database.Observable::class.java.getDeclaredField("mObservers")
        .apply { isAccessible = true }
        .set(observable, ArrayList<Any>())
    return adapter
}

/** Same seeding + handoff AddGenericLongForm.items / ALongForm.setVisibilityOfItems do on open. */
fun openForm(
    tags: Map<String, String> = emptyMap(),
    quests: List<LongFormQuest> = sidewalkQuests(),
): LongFormAdapter<List<LongFormQuest?>> {
    quests.forEach { it.seedFrom(tags) }
    return jvmLongFormAdapter().also { it.items = quests }
}

fun LongFormAdapter<*>.live(questId: Int): LongFormQuest = givenItems.first { it.questId == questId }

fun LongFormAdapter<*>.positionOf(questId: Int): Int {
    val position = items.indexOfFirst { it.questId == questId }
    check(position >= 0) { "question $questId is not currently shown" }
    return position
}

/** A TextEntry row's watcher as TextEntryViewHolder binds it: once, when the row is bound - it is
 *  NOT rebound when the row merely moves, so tests can keep using it after the rows shift. */
fun LongFormAdapter<*>.boundTextEntryWatcher(questId: Int): (String) -> Unit {
    positionOf(questId) // the row must be shown to be bound at all
    val watcher = TextEntryTextWatcher()
    watcher.updateQuestId(questId)
    return { text ->
        watcher.beforeTextChanged(text, 0, 0, text.length)
        watcher.onTextChanged(text, 0, 0, text.length)
        watcher.afterTextChanged(editable(text))
    }
}

/** A Numeric row's watcher as InputViewHolder binds it - see [boundTextEntryWatcher]. */
fun LongFormAdapter<*>.boundNumericWatcher(questId: Int): (String) -> Unit {
    positionOf(questId)
    val watcher = CustomTextWatcher()
    watcher.updateQuestId(questId)
    val validation = live(questId).questAnswerValidation
    // no TextInputLayout on the JVM - the watcher null-checks it and still reports errors via isErrorFree
    watcher.javaClass.getDeclaredField("minValue").apply { isAccessible = true }.set(watcher, validation?.min)
    watcher.javaClass.getDeclaredField("maxValue").apply { isAccessible = true }.setInt(watcher, validation?.max ?: Int.MAX_VALUE)
    return { text ->
        watcher.onTextChanged(text, 0, 0, text.length)
        watcher.afterTextChanged(editable(text))
    }
}

/** Simulates the user typing into a TextEntry row. */
fun LongFormAdapter<*>.typeText(questId: Int, text: String) = boundTextEntryWatcher(questId)(text)

/** Simulates the user typing into a Numeric row. */
fun LongFormAdapter<*>.typeNumber(questId: Int, text: String) = boundNumericWatcher(questId)(text)

/** Simulates tapping a choice tile on/off, the way ImageGridViewHolder's selection listener does. */
fun LongFormAdapter<*>.tapChoice(questId: Int, value: String) {
    val quest = live(questId)
    val index = quest.questAnswerChoices!!.indexOfFirst { it?.value == value }
    val multi = quest.questType == "MultipleChoice"
    if (quest.selectedIndex?.contains(index) == true) {
        deselectChoice(questId, value, index, multi)
    } else {
        if (!multi) quest.selectedIndex?.firstOrNull()?.let { previous ->
            // ImageSelectAdapter deselects the old tile before selecting a new one in single-select
            deselectChoice(questId, quest.questAnswerChoices!![previous]!!.value!!, previous, false)
        }
        selectChoice(questId, value, index, multi)
    }
}

private fun editable(text: String): Editable {
    val editable = Mockito.mock(Editable::class.java)
    on(editable.toString()).thenReturn(text)
    return editable
}
