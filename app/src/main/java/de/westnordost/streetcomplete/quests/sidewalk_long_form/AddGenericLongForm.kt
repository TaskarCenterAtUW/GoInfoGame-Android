package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.ALongForm
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest

class AddGenericLongForm(val quests: List<LongFormQuest?>) : ALongForm<List<LongFormQuest?>>() {
    override val items: List<LongFormQuest?>
        get() = quests.let {
            val copy = mutableListOf<LongFormQuest?>()
            for (quest in it) {
                quest?.userInput = null
                copy.add(quest)
            }
            return copy
        }
}
