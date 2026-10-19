package de.westnordost.streetcomplete.screens.main

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.quest.VisibleQuestsSource
import de.westnordost.streetcomplete.screens.workspaces.FakeWorkspaceRepository
import de.westnordost.streetcomplete.testutils.MockOsmServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The workspace's quest definition is changed on the server (element type "Sidewalk" becomes
 * "Sidewalks") while the user is in the workspace. They switch workspace, re-open the same one and
 * tap the sidewalk. This used to crash (NPE in AbstractQuestForm.onCreate): the quests cached under
 * the old long form outlived MainActivity, so the pin opened a form whose quest type was no longer
 * registered.
 */
@RunWith(AndroidJUnit4::class)
class LongFormChangeTest : MapSyncTestBase() {

    @Test
    fun reopeningWorkspaceAfterLongFormChangedOpensQuestOfNewLongForm() {
        longForm = OLD_LONG_FORM
        val activity = openWorkspace()
        waitUntil("map data downloaded") { mapDownloads() > 0 }
        val oldQuest = awaitQuestOfType(OLD_ELEMENT_TYPE)

        // an admin changes the quest definition, the user goes back to the list via "Switch workspace?"
        longForm = LONG_FORM
        val downloadsBefore = mapDownloads()
        onView(withId(R.id.workspace_container)).perform(click())
        onView(withText(R.string.confirmation_switch_workspace_confirm)).inRoot(isDialog()).perform(click())
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).performClick()
        val reopened = awaitActivity(MainActivity::class.java)

        val source: VisibleQuestsSource = koin.get()
        assertNull("quest of the old long form is still cached", source.get(oldQuest.key))

        waitUntil("map data downloaded again") { mapDownloads() > downloadsBefore }
        val newQuest = awaitQuestOfType(NEW_ELEMENT_TYPE)
        assertEquals(oldQuest.position, newQuest.position)
        tapQuestPin(reopened, newQuest)
        waitUntil("long form of the new quest definition opened") { isDisplayed(SURFACE_Q) }
    }

    private fun mapDownloads() = MockOsmServer.snapshot().requests.count { it == "GET map" }

    private fun awaitQuestOfType(typeName: String): Quest {
        val source: VisibleQuestsSource = koin.get()
        var quest: Quest? = null
        waitUntil("$typeName quest for way ${sidewalk.id} on the map") {
            quest = questFor(source, sidewalk.id)?.takeIf { it.type.name == typeName }
            quest != null
        }
        return quest!!
    }

    companion object {
        const val OLD_ELEMENT_TYPE = "Sidewalk"
        const val NEW_ELEMENT_TYPE = "Sidewalks" // the element type of MapSyncTestBase.LONG_FORM

        // the tester's first quest definition
        val OLD_LONG_FORM = """{
          "version": "3.2.0",
          "recency_period": 90,
          "elements": [{
            "element_type": "$OLD_ELEMENT_TYPE",
            "element_type_icon": "sidewalk",
            "quest_query": "ways with (footway=sidewalk)",
            "quests": [
              {"quest_id": 101, "quest_title": "TextEntry Quest", "quest_description": "This is a TextEntry Quest. Enter some text.",
               "quest_type": "TextEntry", "quest_tag": "ext:text_entry"}
            ]
          }]
        }"""
    }
}
