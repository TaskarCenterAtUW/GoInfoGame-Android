package de.westnordost.streetcomplete.quests

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestLongFormListBinding
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormAdapter
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest
import de.westnordost.streetcomplete.util.logs.Log
import de.westnordost.streetcomplete.view.image_select.ImageSelectAdapter

abstract class ALongForm<T> : AbstractOsmQuestForm<T>() {
    final override val contentLayoutResId = R.layout.quest_long_form_list
    private val binding by contentViewBinding(QuestLongFormListBinding::bind)
    protected lateinit var adapter: LongFormAdapter<T>

    var answerMap : MutableMap<Int, Pair<String, String>> = mutableMapOf()

    override val defaultExpanded = true

    protected abstract val items: List<LongFormItem<T>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter =  LongFormAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
        }
        setVisibilityOfItems()
        binding.recyclerView.adapter = adapter
        binding.submitButton.setOnClickListener {
            val editedItems = adapter.items.map { it.visible }
            print(editedItems.size)
        }
    }

    private fun setVisibilityOfItems() {
        val itemCopy = items
        adapter.items = itemCopy
    }
}

data class LongFormItem<T>(val options: T, val title: String?, val description: String?, val questId : Int?,
    var visible : Boolean = true, var userInput : String? = null)
