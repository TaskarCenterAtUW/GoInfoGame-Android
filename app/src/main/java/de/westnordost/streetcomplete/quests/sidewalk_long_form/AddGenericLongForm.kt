package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.ALongForm
import de.westnordost.streetcomplete.quests.LongFormItem
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest

class AddGenericLongForm(val quests: List<Quest?>?) : ALongForm<Quest>() {
    override val items: List<LongFormItem<Quest>>
        get() = quests.orEmpty().map { LongFormItem(it!!, it.questTitle, it.questDescription) }
}
