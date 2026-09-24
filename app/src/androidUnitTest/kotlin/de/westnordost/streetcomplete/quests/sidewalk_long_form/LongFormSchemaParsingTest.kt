package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerDependency
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.QuestAnswerValidation
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Same Json config WorkspaceViewModel.emitLongFormResponse decodes workspace long forms with. */
class LongFormSchemaParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun quest(body: String): LongFormQuest = json.decodeFromString(body)

    @Test fun `dependency given as a single object with a string required value`() {
        val q = quest("""{"quest_id":2,"quest_answer_dependency":{"question_id":1,"required_value":"other"}}""")
        assertEquals(listOf(QuestAnswerDependency(1, listOf("other"))), q.questAnswerDependency)
    }

    @Test fun `dependency given as an array with an array required value`() {
        val q = quest(
            """{"quest_id":3,"quest_answer_dependency":[
                {"question_id":1,"required_value":["a","b"]},
                {"question_id":2,"required_value":"yes"}
            ]}"""
        )
        assertEquals(
            listOf(QuestAnswerDependency(1, listOf("a", "b")), QuestAnswerDependency(2, listOf("yes"))),
            q.questAnswerDependency
        )
    }

    @Test fun `no dependency`() {
        assertNull(quest("""{"quest_id":1}""").questAnswerDependency)
    }

    @Test fun `choices, validation and follow-up`() {
        val q = quest(
            """{"quest_id":5,"quest_type":"Numeric","quest_tag":"width",
                "quest_answer_validation":{"min":12,"max":240},
                "quest_answer_choices":[{"value":"other","choice_text":"Other","choice_follow_up":"Take a photo"}]}"""
        )
        assertEquals(QuestAnswerValidation(12, 240), q.questAnswerValidation)
        assertEquals("Take a photo", q.questAnswerChoices!!.single()!!.choiceFollowUp)
        assertEquals("width", q.questTag)
    }

    @Test fun `parsed questions start unanswered and visible`() {
        val q = quest("""{"quest_id":1,"quest_type":"TextEntry","quest_tag":"note"}""")
        assertNull(q.userInput)
        assertNull(q.seededAnswer)
        assertNull(q.selectedIndex)
        assertEquals(true, q.visible)
    }

    @Test fun `versioned wrapper with recency period and unknown keys`() {
        val response = json.decodeFromString<LongFormResponse>(
            """{"version":"3.2.0","recency_period":30,"something_new":true,
                "elements":[{"element_type":"Sidewalks","quest_query":"ways with highway=footway",
                  "quests":[{"quest_id":101,"quest_type":"ExclusiveChoice","quest_tag":"ext:surface"}]}]}"""
        )
        assertEquals(30, response.recencyPeriodInDays)
        val element = response.elements.single()
        assertEquals("Sidewalks", element.elementType)
        assertEquals(101, element.quests.single()!!.questId)
    }

    @Test fun `legacy bare array of elements`() {
        val elements = json.decodeFromString<List<Elements>>(
            """[{"element_type":"Kerb","quest_query":"nodes with barrier=kerb","quests":[]}]"""
        )
        assertEquals("Kerb", elements.single().elementType)
    }
}
