package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.runtime.Immutable
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.data.quest.QuestType

/** A single previously-hidden quest instance, to be shown in the restore list */
@Immutable
data class HiddenQuest(
    val key: QuestKey,
    val questType: QuestType?,
    val timestamp: Long,
)
