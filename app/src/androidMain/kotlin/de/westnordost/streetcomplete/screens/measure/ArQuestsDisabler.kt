package de.westnordost.streetcomplete.screens.measure

import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.visiblequests.VisibleEditTypeController

class ArQuestsDisabler(
    private val questTypeRegistry: QuestTypeRegistry,
    private val visibleEditTypeController: VisibleEditTypeController
) {
    private val arQuestNames = emptyList<String>()

    fun hideAllArQuests() {
        val arQuests = arQuestNames.mapNotNull { questTypeRegistry.getByName(it) }
        visibleEditTypeController.setVisibilities(arQuests.associateWith { false })
    }
}
