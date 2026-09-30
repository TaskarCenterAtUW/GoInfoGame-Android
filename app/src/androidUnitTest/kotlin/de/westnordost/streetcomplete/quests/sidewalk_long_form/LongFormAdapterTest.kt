package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.PhotoAttachment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerValidation
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LongFormAdapterTest {

    private fun shownIds(adapter: LongFormAdapter<*>) = adapter.items.map { it.questId }

    //region visibility

    @Test fun `dependent questions are hidden until their controlling answer matches`() {
        val adapter = openForm()
        assertEquals(listOf(SURFACE, WIDTH, OBSTRUCTION), shownIds(adapter))

        adapter.tapChoice(SURFACE, "other")
        adapter.tapChoice(OBSTRUCTION, "yes")
        assertEquals(listOf(SURFACE, SURFACE_DESCRIPTION, WIDTH, OBSTRUCTION, OBSTRUCTION_TYPE), shownIds(adapter))

        adapter.tapChoice(SURFACE, "asphalt")
        assertEquals(listOf(SURFACE, WIDTH, OBSTRUCTION, OBSTRUCTION_TYPE), shownIds(adapter))
    }

    @Test fun `pre-filled controlling answer shows its dependent question on open`() {
        val adapter = openForm(mapOf("ext:surface" to "other"))
        assertTrue(SURFACE_DESCRIPTION in shownIds(adapter))
        assertTrue(adapter.live(SURFACE_DESCRIPTION).visible)
    }

    @Test fun `hiding a dependent question keeps what was typed so toggling back restores it`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "other")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")

        adapter.tapChoice(SURFACE, "asphalt")
        assertFalse(adapter.live(SURFACE_DESCRIPTION).visible)
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).userInput)

        adapter.tapChoice(SURFACE, "other")
        assertEquals("cobbles", (adapter.items[adapter.positionOf(SURFACE_DESCRIPTION)].userInput as UserInput.Single).answer)
    }

    @Test fun `hiding a dependent choice question clears its tile selection and its answer`() {
        val adapter = openForm()
        adapter.tapChoice(OBSTRUCTION, "yes")
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION, "no")
        assertNull(adapter.live(OBSTRUCTION_TYPE).selectedIndex)
        assertNull(adapter.live(OBSTRUCTION_TYPE).userInput)
    }

    @Test fun `a re-shown choice question starts blank and does not merge in the old answer`() {
        val adapter = openForm()
        adapter.tapChoice(OBSTRUCTION, "yes")
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION, "no")
        adapter.tapChoice(OBSTRUCTION, "yes")
        assertNull(adapter.live(OBSTRUCTION_TYPE).userInput)

        adapter.tapChoice(OBSTRUCTION_TYPE, "pole")
        assertEquals(UserInput.Multiple(mutableListOf("pole")), adapter.live(OBSTRUCTION_TYPE).userInput)
        assertEquals<List<Int>?>(listOf(1), adapter.live(OBSTRUCTION_TYPE).selectedIndex)
    }

    @Test fun `hiding a dependent exclusive choice question clears its answer`() {
        val quests = listOf(
            LongFormQuest(
                questId = 1, questTag = "a", questType = "ExclusiveChoice",
                questAnswerChoices = listOf(choice("yes"), choice("no")),
            ),
            LongFormQuest(
                questId = 2, questTag = "b", questType = "ExclusiveChoice",
                questAnswerDependency = dependsOn(1, "yes"),
                questAnswerChoices = listOf(choice("x"), choice("y")),
            ),
        )
        val adapter = openForm(quests = quests)
        adapter.tapChoice(1, "yes")
        adapter.tapChoice(2, "x")
        adapter.tapChoice(1, "no")
        assertNull(adapter.live(2).userInput)
        assertNull(adapter.live(2).selectedIndex)
    }

    @Test fun `re-rendering keeps the same live question list`() {
        val quests = sidewalkQuests()
        val adapter = openForm(quests = quests)
        adapter.tapChoice(SURFACE, "other") // SURFACE controls a dependency, so this re-renders
        assertSame(quests, adapter.givenItems)
    }

    @Test fun `rendered items are snapshots, not the live questions`() {
        val adapter = openForm()
        adapter.items.forEach { snapshot ->
            assertFalse(adapter.givenItems.any { it === snapshot })
        }
    }

    @Test fun `setting the photo attachment does not disturb the questions`() {
        val adapter = openForm(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "other"))
        val before = shownIds(adapter)
        adapter.photoAttachment = PhotoAttachment.Pending("/tmp/p.jpg")
        adapter.photoAttachment = PhotoAttachment.None
        assertEquals(before, shownIds(adapter))
    }

    //endregion

    //region text entry (regression: typed text was written to a snapshot and never submitted)

    @Test fun `typed text entry answer lands in the live question`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "other")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).userInput)
    }

    @Test fun `typing over a pre-filled text entry replaces the live answer, not the seeded one`() {
        val adapter = openForm(FULLY_ANSWERED_TAGS)
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles and tar")
        assertEquals(UserInput.Single("cobbles and tar"), adapter.live(SURFACE_DESCRIPTION).userInput)
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).seededAnswer)
    }

    @Test fun `each keystroke overwrites the previous text`() {
        val adapter = openForm(mapOf("ext:surface" to "other"))
        listOf("c", "co", "cob").forEach { adapter.typeText(SURFACE_DESCRIPTION, it) }
        assertEquals(UserInput.Single("cob"), adapter.live(SURFACE_DESCRIPTION).userInput)
    }

    @Test fun `text watcher targets the right question after a dependency reveal shifts rows`() {
        val adapter = openForm()
        val widthRowBefore = adapter.positionOf(WIDTH)
        adapter.tapChoice(SURFACE, "other") // inserts SURFACE_DESCRIPTION above WIDTH
        assertEquals(widthRowBefore + 1, adapter.positionOf(WIDTH))

        adapter.typeNumber(WIDTH, "60")
        adapter.typeText(SURFACE_DESCRIPTION, "cobbles")
        assertEquals(UserInput.Single("60"), adapter.live(WIDTH).userInput)
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).userInput)
    }

    @Test fun `a watcher bound before its row moved still writes to its own question`() {
        // regression: watchers used to remember the bind-time adapter position, and a row that
        // only moves isn't rebound - typing into width after the description appeared above it
        // overwrote the description instead
        val adapter = openForm()
        val widthWatcher = adapter.boundNumericWatcher(WIDTH)
        adapter.tapChoice(SURFACE, "other") // inserts SURFACE_DESCRIPTION above WIDTH, no rebind
        val descriptionWatcher = adapter.boundTextEntryWatcher(SURFACE_DESCRIPTION)

        widthWatcher("60")
        descriptionWatcher("cobbles")
        widthWatcher("72")
        assertEquals(UserInput.Single("72"), adapter.live(WIDTH).userInput)
        assertEquals(UserInput.Single("cobbles"), adapter.live(SURFACE_DESCRIPTION).userInput)
    }

    @Test fun `a moved numeric row reports validation errors for its own question`() {
        val adapter = openForm()
        val widthWatcher = adapter.boundNumericWatcher(WIDTH)
        adapter.tapChoice(SURFACE, "other")
        widthWatcher("999")
        assertFalse(adapter.isErrorFree.value)
        adapter.tapChoice(SURFACE, "asphalt") // hides the description - must not clear width's error
        assertFalse(adapter.isErrorFree.value)
    }

    @Test fun `text entry is not numerically validated`() {
        val adapter = openForm(mapOf("ext:surface" to "other"))
        adapter.typeText(SURFACE_DESCRIPTION, "not a number")
        assertTrue(adapter.isErrorFree.value)
    }

    //endregion

    //region numeric

    @Test fun `typed numeric answer lands in the live question`() {
        val adapter = openForm(FULLY_ANSWERED_TAGS)
        adapter.typeNumber(WIDTH, "72")
        assertEquals(UserInput.Single("72"), adapter.live(WIDTH).userInput)
        assertEquals(UserInput.Single("60"), adapter.live(WIDTH).seededAnswer)
    }

    @Test fun `numeric values within range are error free`() {
        val adapter = openForm()
        listOf("12", "240", "60.5").forEach {
            adapter.typeNumber(WIDTH, it)
            assertTrue(adapter.isErrorFree.value, it)
        }
    }

    @Test fun `numeric values out of range or not numbers block submit`() {
        val adapter = openForm()
        listOf("11", "241", "abc", "-5").forEach {
            adapter.typeNumber(WIDTH, it)
            assertFalse(adapter.isErrorFree.value, it)
        }
    }

    @Test fun `correcting or clearing a numeric value unblocks submit`() {
        val adapter = openForm()
        adapter.typeNumber(WIDTH, "999")
        assertFalse(adapter.isErrorFree.value)
        adapter.typeNumber(WIDTH, "100")
        assertTrue(adapter.isErrorFree.value)
        adapter.typeNumber(WIDTH, "999")
        adapter.typeNumber(WIDTH, "")
        assertTrue(adapter.isErrorFree.value)
    }

    @Test fun `numeric without validation accepts any non-negative number`() {
        val quests = listOf(LongFormQuest(questId = 1, questTag = "n", questType = "Numeric"))
        val adapter = openForm(quests = quests)
        adapter.typeNumber(1, "100000")
        assertTrue(adapter.isErrorFree.value)
    }

    @Test fun `an invalid numeric answer that gets hidden no longer blocks submit`() {
        val quests = listOf(
            LongFormQuest(
                questId = 1, questTag = "has_steps", questType = "ExclusiveChoice",
                questAnswerChoices = listOf(choice("yes"), choice("no")),
            ),
            LongFormQuest(
                questId = 2, questTag = "step_count", questType = "Numeric",
                questAnswerDependency = dependsOn(1, "yes"),
                questAnswerValidation = QuestAnswerValidation(min = 1, max = 10),
            ),
        )
        val adapter = openForm(quests = quests)
        adapter.tapChoice(1, "yes")
        adapter.typeNumber(2, "99")
        assertFalse(adapter.isErrorFree.value)

        adapter.tapChoice(1, "no")
        assertTrue(adapter.isErrorFree.value)
    }

    //endregion

    //region choices

    @Test fun `exclusive choice keeps only the latest selection`() {
        val adapter = openForm()
        adapter.tapChoice(SURFACE, "asphalt")
        adapter.tapChoice(SURFACE, "concrete")
        assertEquals(UserInput.Single("concrete"), adapter.live(SURFACE).userInput)
        assertEquals<List<Int>?>(listOf(1), adapter.live(SURFACE).selectedIndex)
    }

    @Test fun `deselecting an exclusive choice clears the answer`() {
        val adapter = openForm(mapOf("ext:surface" to "asphalt"))
        adapter.tapChoice(SURFACE, "asphalt")
        assertNull(adapter.live(SURFACE).userInput)
        assertEquals<List<Int>?>(emptyList(), adapter.live(SURFACE).selectedIndex)
    }

    @Test fun `multiple choice accumulates and removes selections`() {
        val adapter = openForm(mapOf("ext:obstruction" to "yes"))
        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        adapter.tapChoice(OBSTRUCTION_TYPE, "pole")
        assertEquals(UserInput.Multiple(mutableListOf("bollard", "pole")), adapter.live(OBSTRUCTION_TYPE).userInput)

        adapter.tapChoice(OBSTRUCTION_TYPE, "bollard")
        assertEquals(UserInput.Multiple(mutableListOf("pole")), adapter.live(OBSTRUCTION_TYPE).userInput)
        assertEquals<List<Int>?>(listOf(1), adapter.live(OBSTRUCTION_TYPE).selectedIndex)
    }

    @Test fun `selecting on a pre-filled multiple choice adds to the seeded answers`() {
        val adapter = openForm(mapOf("ext:obstruction" to "yes", "ext:obstruction:type" to "bollard"))
        adapter.tapChoice(OBSTRUCTION_TYPE, "pole")
        assertEquals(UserInput.Multiple(mutableListOf("bollard", "pole")), adapter.live(OBSTRUCTION_TYPE).userInput)
        assertEquals(UserInput.Multiple(mutableListOf("bollard")), adapter.live(OBSTRUCTION_TYPE).seededAnswer)
    }

    //endregion
}
