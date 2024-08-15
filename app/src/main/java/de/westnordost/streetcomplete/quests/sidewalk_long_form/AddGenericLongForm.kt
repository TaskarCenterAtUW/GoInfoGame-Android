package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.ALongForm
import de.westnordost.streetcomplete.quests.LongFormItem
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest

class AddGenericLongForm(val quests: List<Quest?>) : ALongForm<List<Quest?>>() {
    override val items: List<Quest?>
        get() = quests.let {
            val copy = mutableListOf<Quest?>()
            for (quest in it) {
                quest?.userInput = null
                copy.add(quest)
            }
            return copy
        }

}
