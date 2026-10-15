package de.westnordost.streetcomplete.screens.user

import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.quest.TestQuestTypeA
import de.westnordost.streetcomplete.data.quest.TestQuestTypeB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShowQuestFormsActionTest {

    @Test fun `hidden before any workspace is loaded`() {
        val registry = QuestTypeRegistry(emptyList())

        assertNull(showQuestFormsActionOrNull(registry) {})
    }

    @Test fun `shown once a workspace has loaded its quest types`() {
        val registry = QuestTypeRegistry(emptyList())
        // what MainActivity does when a workspace loads
        registry.addItem(listOf(0 to TestQuestTypeA(), 1 to TestQuestTypeB()))

        var launched = 0
        val action = showQuestFormsActionOrNull(registry) { launched++ }

        assertNotNull(action)
        action()
        assertEquals(1, launched)
    }

    @Test fun `hidden again after going back to the workspace list`() {
        val registry = QuestTypeRegistry(listOf(0 to TestQuestTypeA()))
        // what WorkSpaceActivity does when the workspace list opens
        registry.addItem(emptyList())

        assertNull(showQuestFormsActionOrNull(registry) {})
    }
}
