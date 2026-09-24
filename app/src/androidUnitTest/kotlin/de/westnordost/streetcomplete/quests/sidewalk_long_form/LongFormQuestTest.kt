package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerDependency
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.activeChoiceFollowUp
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.contentEquals
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.isVisibleGiven
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.seedFrom
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LongFormQuestTest {

    private fun quest(id: Int) = sidewalkQuests().first { it.questId == id }

    //region seedFrom

    @Test fun `seedFrom pre-fills an exclusive choice and selects its tile`() {
        val q = quest(SURFACE)
        q.seedFrom(mapOf("ext:surface" to "concrete"))
        assertEquals(UserInput.Single("concrete"), q.userInput)
        assertEquals(UserInput.Single("concrete"), q.seededAnswer)
        assertEquals<List<Int>?>(listOf(1), q.selectedIndex)
    }

    @Test fun `seedFrom splits a multiple choice value on semicolons`() {
        val q = quest(OBSTRUCTION_TYPE)
        q.seedFrom(mapOf("ext:obstruction:type" to "bollard;other"))
        assertEquals(UserInput.Multiple(mutableListOf("bollard", "other")), q.userInput)
        assertEquals<List<Int>?>(listOf(0, 2), q.selectedIndex)
    }

    @Test fun `seedFrom pre-fills text entry and numeric questions`() {
        val text = quest(SURFACE_DESCRIPTION)
        text.seedFrom(mapOf("ext:surface:description" to "old cobbles"))
        assertEquals(UserInput.Single("old cobbles"), text.userInput)
        assertEquals(UserInput.Single("old cobbles"), text.seededAnswer)
        assertNull(text.selectedIndex)

        val numeric = quest(WIDTH)
        numeric.seedFrom(mapOf("width" to "48"))
        assertEquals(UserInput.Single("48"), numeric.userInput)
    }

    @Test fun `seedFrom with a value matching no choice keeps the answer but selects nothing`() {
        val q = quest(SURFACE)
        q.seedFrom(mapOf("ext:surface" to "wood"))
        assertEquals(UserInput.Single("wood"), q.seededAnswer)
        assertNull(q.selectedIndex)
    }

    @Test fun `seedFrom fully resets a reused instance when the next element lacks the tag`() {
        // quest instances are shared across every element of a type - a stale answer must not leak
        val q = quest(SURFACE)
        q.seedFrom(mapOf("ext:surface" to "asphalt"))
        q.seedFrom(emptyMap())
        assertNull(q.userInput)
        assertNull(q.seededAnswer)
        assertNull(q.selectedIndex)
    }

    @Test fun `seedFrom also resets in-session edits made on a previous open`() {
        val q = quest(SURFACE_DESCRIPTION)
        q.seedFrom(emptyMap())
        q.userInput = UserInput.Single("typed but never submitted")
        q.seedFrom(emptyMap())
        assertNull(q.userInput)
    }

    @Test fun `seededAnswer is not affected by later in-place edits of userInput`() {
        val q = quest(OBSTRUCTION_TYPE)
        q.seedFrom(mapOf("ext:obstruction:type" to "bollard"))
        (q.userInput as UserInput.Multiple).answers.add("pole")
        assertEquals(UserInput.Multiple(mutableListOf("bollard")), q.seededAnswer)
    }

    //endregion

    //region contentEquals

    @Test fun `contentEquals compares single answers by value`() {
        assertTrue(UserInput.Single("a").contentEquals(UserInput.Single("a")))
        assertFalse(UserInput.Single("a").contentEquals(UserInput.Single("b")))
    }

    @Test fun `contentEquals ignores the order of multiple answers`() {
        assertTrue(
            UserInput.Multiple(mutableListOf("a", "b"))
                .contentEquals(UserInput.Multiple(mutableListOf("b", "a")))
        )
        assertFalse(
            UserInput.Multiple(mutableListOf("a"))
                .contentEquals(UserInput.Multiple(mutableListOf("a", "b")))
        )
    }

    @Test fun `contentEquals treats a cleared answer as different from a never-answered one`() {
        // this is what makes clearing a pre-filled field count as a change to submit
        assertFalse(UserInput.Single("").contentEquals(null))
        assertFalse(null.contentEquals(UserInput.Single("x")))
        assertTrue(null.contentEquals(null))
    }

    @Test fun `contentEquals treats single and multiple as different`() {
        assertFalse(UserInput.Single("a").contentEquals(UserInput.Multiple(mutableListOf("a"))))
    }

    @Test fun `isEmpty`() {
        assertTrue(UserInput.Single(null).isEmpty())
        assertTrue(UserInput.Single("").isEmpty())
        assertFalse(UserInput.Single("x").isEmpty())
        assertTrue(UserInput.Multiple().isEmpty())
        assertFalse(UserInput.Multiple(mutableListOf("x")).isEmpty())
    }

    //endregion

    //region isVisibleGiven

    private fun LongFormQuest.visibleWith(answers: Map<Int, List<String>>, existing: Set<Int> = setOf(1, 2)) =
        isVisibleGiven({ it in existing }, { answers[it] })

    @Test fun `question without dependencies is always visible`() {
        assertTrue(LongFormQuest(questId = 3).visibleWith(emptyMap()))
    }

    @Test fun `dependent question is visible only once the controlling answer matches`() {
        val q = LongFormQuest(questId = 3, questAnswerDependency = dependsOn(1, "yes"))
        assertFalse(q.visibleWith(emptyMap()))
        assertFalse(q.visibleWith(mapOf(1 to listOf("no"))))
        assertTrue(q.visibleWith(mapOf(1 to listOf("yes"))))
    }

    @Test fun `any of several required values satisfies the dependency`() {
        val q = LongFormQuest(questId = 3, questAnswerDependency = dependsOn(1, "a", "b"))
        assertTrue(q.visibleWith(mapOf(1 to listOf("b"))))
    }

    @Test fun `a multiple choice controlling answer satisfies the dependency if any selected value matches`() {
        val q = LongFormQuest(questId = 3, questAnswerDependency = dependsOn(1, "other"))
        assertTrue(q.visibleWith(mapOf(1 to listOf("bollard", "other"))))
    }

    @Test fun `all dependencies must be satisfied`() {
        val q = LongFormQuest(
            questId = 3,
            questAnswerDependency = dependsOn(1, "yes") + dependsOn(2, "yes"),
        )
        assertFalse(q.visibleWith(mapOf(1 to listOf("yes"))))
        assertTrue(q.visibleWith(mapOf(1 to listOf("yes"), 2 to listOf("yes"))))
    }

    @Test fun `dependency on a question not in the schema is ignored`() {
        val q = LongFormQuest(questId = 3, questAnswerDependency = dependsOn(99, "yes"))
        assertTrue(q.visibleWith(emptyMap()))
    }

    @Test fun `incomplete dependency entries are ignored`() {
        val q = LongFormQuest(
            questId = 3,
            questAnswerDependency = listOf(
                QuestAnswerDependency(questionId = 1, requiredValue = null),
                QuestAnswerDependency(questionId = null, requiredValue = listOf("yes")),
            ),
        )
        assertTrue(q.visibleWith(emptyMap()))
    }

    //endregion

    //region snapshot / activeChoiceFollowUp

    @Test fun `snapshot does not share mutable lists with the live quest`() {
        val q = quest(OBSTRUCTION_TYPE)
        q.seedFrom(mapOf("ext:obstruction:type" to "bollard"))
        val snap = q.snapshot()
        q.selectedIndex!!.add(1)
        (q.userInput as UserInput.Multiple).answers.add("pole")
        assertEquals<List<Int>?>(listOf(0), snap.selectedIndex)
        assertEquals(UserInput.Multiple(mutableListOf("bollard")), snap.userInput)
        assertNotSame(q.userInput, snap.userInput)
    }

    @Test fun `activeChoiceFollowUp is the follow-up of a selected choice`() {
        val q = quest(OBSTRUCTION_TYPE)
        q.selectedIndex = mutableListOf(0)
        assertNull(q.activeChoiceFollowUp())
        q.selectedIndex = mutableListOf(0, 2)
        assertEquals(PHOTO_FOLLOW_UP, q.activeChoiceFollowUp())
    }

    @Test fun `blank follow-up counts as none`() {
        val q = LongFormQuest(questAnswerChoices = listOf(choice("a", "  ")), selectedIndex = mutableListOf(0))
        assertNull(q.activeChoiceFollowUp())
    }

    //endregion
}
