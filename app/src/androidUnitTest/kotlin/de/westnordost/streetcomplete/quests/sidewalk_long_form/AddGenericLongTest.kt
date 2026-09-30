package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.content.res.Resources
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.way
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddGenericLongTest {

    private val sidewalkTags = mapOf("highway" to "footway", "footway" to "sidewalk")
    private val day = 24L * 60 * 60 * 1000

    @BeforeTest fun setUp() {
        startKoin { modules(module { single<Resources> { mock() } }) }
    }

    @AfterTest fun tearDown() {
        stopKoin()
    }

    private fun questType(recencyDays: Int = 90) = AddGenericLong(
        Elements(
            elementType = "Sidewalks",
            questQuery = "ways with highway=footway and footway=sidewalk",
            quests = sidewalkQuests(),
        ),
        recencyDays
    )

    private fun sidewalk(tags: Map<String, String> = emptyMap(), editedDaysAgo: Int = 0) =
        way(tags = sidewalkTags + tags, timestamp = nowAsEpochMilliseconds() - editedDaysAgo * day)

    //region isApplicableTo

    @Test fun `not applicable to elements outside the quest query`() {
        assertFalse(questType().isApplicableTo(node(tags = sidewalkTags)))
        assertFalse(questType().isApplicableTo(way(tags = mapOf("highway" to "residential"))))
    }

    @Test fun `applicable while any question is unanswered`() {
        assertTrue(questType().isApplicableTo(sidewalk()))
        assertTrue(questType().isApplicableTo(sidewalk(FULLY_ANSWERED_TAGS - "width")))
    }

    @Test fun `a blank tag counts as unanswered`() {
        assertTrue(questType().isApplicableTo(sidewalk(FULLY_ANSWERED_TAGS + ("width" to " "))))
    }

    @Test fun `a dependent question whose condition is not met does not count as unanswered`() {
        val tags = mapOf("ext:surface" to "asphalt", "width" to "60", "ext:obstruction" to "no")
        assertFalse(questType().isApplicableTo(sidewalk(tags)))
    }

    @Test fun `a dependent question whose condition is met counts as unanswered`() {
        val tags = mapOf("ext:surface" to "other", "width" to "60", "ext:obstruction" to "no")
        assertTrue(questType().isApplicableTo(sidewalk(tags)))
    }

    @Test fun `a multiple choice controlling answer is split on semicolons`() {
        val quests = listOf(
            LongFormQuest(questId = 1, questTag = "a", questType = "MultipleChoice"),
            LongFormQuest(questId = 2, questTag = "b", questAnswerDependency = dependsOn(1, "y")),
        )
        val type = AddGenericLong(Elements(questQuery = "ways", quests = quests), 90)
        assertTrue(type.isApplicableTo(way(tags = mapOf("a" to "x;y"))))
        assertFalse(type.isApplicableTo(way(tags = mapOf("a" to "x;z"))))
    }

    @Test fun `fully answered and recently edited is not applicable`() {
        assertFalse(questType(recencyDays = 90).isApplicableTo(sidewalk(FULLY_ANSWERED_TAGS, editedDaysAgo = 89)))
    }

    @Test fun `fully answered but older than the recency period resurfaces for a recheck`() {
        assertTrue(questType(recencyDays = 90).isApplicableTo(sidewalk(FULLY_ANSWERED_TAGS, editedDaysAgo = 91)))
    }

    @Test fun `recency period of zero always resurfaces`() {
        assertTrue(questType(recencyDays = 0).isApplicableTo(sidewalk(FULLY_ANSWERED_TAGS)))
    }

    //endregion

    //region applyAnswerTo

    private fun apply(vararg answers: LongFormQuest?, existing: Map<String, String> = emptyMap()): Tags {
        val tags = Tags(existing)
        questType().applyAnswerTo(answers.toList(), tags, ElementPointGeometry(LatLon(0.0, 0.0)), 0L)
        return tags
    }

    private fun answer(tag: String?, input: UserInput?) = LongFormQuest(questTag = tag, userInput = input)

    @Test fun `single answer sets the tag`() {
        val tags = apply(answer("ext:surface", UserInput.Single("asphalt")), existing = mapOf("ext:surface" to "concrete"))
        assertEquals("asphalt", tags["ext:surface"])
    }

    @Test fun `text entry answer is written verbatim`() {
        val tags = apply(answer("ext:surface:description", UserInput.Single("broken; uneven slabs")))
        assertEquals("broken; uneven slabs", tags["ext:surface:description"])
    }

    @Test fun `multiple answers are joined with semicolons`() {
        val tags = apply(answer("ext:obstruction:type", UserInput.Multiple(mutableListOf("bollard", "pole"))))
        assertEquals("bollard;pole", tags["ext:obstruction:type"])
    }

    @Test fun `cleared answers remove the tag`() {
        val existing = mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4")
        val tags = apply(
            answer("a", UserInput.Single("")),
            answer("b", UserInput.Single(null)),
            answer("c", UserInput.Multiple()),
            answer("d", null),
            existing = existing,
        )
        assertEquals(emptyMap(), tags.toMap())
    }

    @Test fun `removing a tag that does not exist is harmless`() {
        val tags = apply(answer("a", null))
        assertTrue(tags.create().isEmpty())
    }

    @Test fun `null quests and quests without a tag are skipped`() {
        val tags = apply(null, answer(null, UserInput.Single("x")), existing = mapOf("k" to "v"))
        assertTrue(tags.create().isEmpty())
    }

    //endregion
}
