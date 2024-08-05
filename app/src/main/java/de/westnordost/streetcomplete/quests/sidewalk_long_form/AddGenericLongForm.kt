package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.quests.ALongForm
import de.westnordost.streetcomplete.quests.LongFormItem

class AddGenericLongForm : ALongForm<String>() {
    override val items: List<LongFormItem<String>>
        get() = emptyList()
}
