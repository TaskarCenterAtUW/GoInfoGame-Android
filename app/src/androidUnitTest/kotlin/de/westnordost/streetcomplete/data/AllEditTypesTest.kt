package de.westnordost.streetcomplete.data

import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.quest.TestQuestTypeA
import de.westnordost.streetcomplete.data.quest.TestQuestTypeB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class AllEditTypesTest {

    private val questA = TestQuestTypeA()
    private val allEditTypes = AllEditTypes(mutableListOf(QuestTypeRegistry(listOf(0 to questA))))

    @Test
    fun `returns null for unknown name when no fallback is set`() {
        assertNull(allEditTypes.getByName("Removed"))
    }

    @Test
    fun `registered name resolves to registry type, not fallback`() {
        allEditTypes.unknownTypeFallback = { NamedQuestType(it) }
        assertSame(questA, allEditTypes.getByName("TestQuestTypeA"))
    }

    @Test
    fun `unknown name resolves to fallback type with that name`() {
        // e.g. an edit stored for a long-form element type later removed from the long form
        allEditTypes.unknownTypeFallback = { NamedQuestType(it) }
        assertEquals("Removed", allEditTypes.getByName("Removed")?.name)
    }

    @Test
    fun `fallback is created once per name`() {
        var calls = 0
        allEditTypes.unknownTypeFallback = { calls++; NamedQuestType(it) }
        val first = allEditTypes.getByName("Removed")
        assertSame(first, allEditTypes.getByName("Removed"))
        assertEquals(1, calls)
    }

    @Test
    fun `name re-added to registry wins over cached fallback`() {
        allEditTypes.unknownTypeFallback = { NamedQuestType(it) }
        allEditTypes.getByName("TestQuestTypeB")

        val questB = TestQuestTypeB()
        allEditTypes.registries.add(QuestTypeRegistry(listOf(0 to questB)))
        allEditTypes.updateByName()

        assertSame(questB, allEditTypes.getByName("TestQuestTypeB"))
    }
}

private class NamedQuestType(override val name: String) : TestQuestTypeA()
