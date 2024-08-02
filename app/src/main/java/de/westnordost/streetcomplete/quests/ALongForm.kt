package de.westnordost.streetcomplete.quests

import android.os.Bundle
import android.view.View
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)

    override val defaultExpanded = false

    protected abstract val items: List<LongFormItem<T>>
    // val item = LongFormItem(
    //     listOf("Asphalt", "Concrete", "Brick"), "What type of surface is the sidewalk?",
    //     "Choose the type of surface of the sidewalk"
    // )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}

data class LongFormItem<T>(val options: T, val title: String, val description: String)
