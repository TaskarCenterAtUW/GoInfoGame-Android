package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.graphics.Rect
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.textfield.TextInputLayout
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerChoice
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerDependency
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerValidation
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/* Same Sidewalks schema as the unit tests' fixture (and WorkspaceViewModel's
 * DEFAULT_TEST_LONG_FORM_JSON): every question type, a dependency and a photo follow-up. Titles
 * and choice texts are what the tests look views up by. */
const val SURFACE_Q = "What is the surface?"
const val DESCRIPTION_Q = "Describe the surface"
const val WIDTH_Q = "How wide is it, in inches?"
const val OBSTRUCTION_Q = "Any obstructions?"
const val OBSTRUCTION_TYPE_Q = "What obstructions?"
const val PHOTO_FOLLOW_UP = "Please take a photo of the obstruction."
const val OTHER_OBSTRUCTION = "Other obstruction"

private fun choice(value: String, text: String, followUp: String? = null) =
    QuestAnswerChoice(value = value, choiceText = text, choiceFollowUp = followUp)

private fun dependsOn(questId: Int, value: String) =
    listOf(QuestAnswerDependency(questionId = questId, requiredValue = listOf(value)))

fun sidewalkQuests(): List<LongFormQuest> = listOf(
    LongFormQuest(
        questId = 101, questTitle = SURFACE_Q, questTag = "ext:surface", questType = "ExclusiveChoice",
        questAnswerChoices = listOf(choice("asphalt", "Asphalt"), choice("concrete", "Concrete"), choice("other", "Other")),
    ),
    LongFormQuest(
        questId = 102, questTitle = DESCRIPTION_Q, questTag = "ext:surface:description", questType = "TextEntry",
        questAnswerDependency = dependsOn(101, "other"),
    ),
    LongFormQuest(
        questId = 103, questTitle = WIDTH_Q, questTag = "width", questType = "Numeric",
        questAnswerValidation = QuestAnswerValidation(min = 12, max = 240),
    ),
    LongFormQuest(
        questId = 104, questTitle = OBSTRUCTION_Q, questTag = "ext:obstruction", questType = "ExclusiveChoice",
        questAnswerChoices = listOf(choice("yes", "Yes"), choice("no", "No")),
    ),
    LongFormQuest(
        questId = 105, questTitle = OBSTRUCTION_TYPE_Q, questTag = "ext:obstruction:type", questType = "MultipleChoice",
        questAnswerDependency = dependsOn(104, "yes"),
        questAnswerChoices = listOf(
            choice("bollard", "Bollard"),
            choice("pole", "Utility pole"),
            choice("other", OTHER_OBSTRUCTION, PHOTO_FOLLOW_UP),
        ),
    ),
)

val FULLY_ANSWERED_TAGS = mapOf(
    "ext:surface" to "other",
    "ext:surface:description" to "cobbles",
    "width" to "60",
    "ext:obstruction" to "yes",
    "ext:obstruction:type" to "bollard;pole",
)

//region matchers

/** The row (cell root) of the question titled [title]. */
fun questionRow(title: String): Matcher<View> =
    allOf(withId(R.id.container), hasDescendant(allOf(withId(R.id.title), withText(title))))

/** The text/number field of the question titled [title]. */
fun fieldOf(title: String): Matcher<View> =
    allOf(withId(R.id.editText), isDescendantOfA(questionRow(title)))

/** The tile showing [choiceText] in the question titled [title]. */
fun tileOf(title: String, choiceText: String): Matcher<View> = allOf(
    withParent(allOf(withId(R.id.list), isDescendantOfA(questionRow(title)))),
    hasDescendant(withText(choiceText)),
)

fun inRowOf(title: String, viewId: Int): Matcher<View> =
    allOf(withId(viewId), isDescendantOfA(questionRow(title)))

fun hasInputError(error: String?): Matcher<View> =
    object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
        override fun describeTo(description: Description) { description.appendText("has input error \"$error\"") }
        override fun matchesSafely(item: TextInputLayout) = item.error?.toString() == error
    }

fun hasAlpha(alpha: Float): Matcher<View> =
    object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) { description.appendText("has alpha $alpha") }
        override fun matchesSafely(item: View) = item.alpha == alpha
    }

//endregion

/** Brings a view inside the quest sheet's NestedScrollView on screen - Espresso's own scrollTo()
 *  only knows ScrollView/HorizontalScrollView/ListView ancestors. */
fun scrollIntoView(): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDescendantOfA(isAssignableFrom(NestedScrollView::class.java))
    override fun getDescription() = "scroll into view inside NestedScrollView"
    override fun perform(uiController: UiController, view: View) {
        view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
        uiController.loopMainThreadUntilIdle()
    }
}
