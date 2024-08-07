package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.chip.Chip
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.CellLongFormItemBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemExclusiveChoiceBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemInputBinding
import de.westnordost.streetcomplete.quests.LongFormItem
import de.westnordost.streetcomplete.util.Listeners

class LongFormAdapter<T> :
    RecyclerView.Adapter<ViewHolder>() {
    var givenItems = listOf<LongFormItem<T>>()
    var needRefreshIds = listOf<Int?>()
    var items = listOf<LongFormItem<T>>()
        set(value) {
            if (givenItems.isEmpty()) {
                givenItems = value
                needRefreshIds = givenItems.map { (it.options as Quest).questAnswerDependency?.questionId }
            }

            field = manageVisibility(value).filter { it.visible }
            notifyDataSetChanged()
        }

    var cellLayoutId = R.layout.cell_long_form_item

    enum class ViewType(val value: Int) {
        EXCLUSIVE_CHOICE(1),
        INPUT(2),
        DEFAULT(3);

        companion object {
            fun fromInt(value: Int): ViewType? = entries.find { it.value == value }
        }
    }


    fun manageVisibility(itemCopy : List<LongFormItem<T>>): List<LongFormItem<T>> {
        for (item in itemCopy) {
            val quest = item.options as Quest

            val requiredUserInput= quest.questAnswerDependency?.requiredValue
            val requiredQuestId = quest.questAnswerDependency?.questionId


            if (requiredUserInput == null || requiredQuestId == null){
                itemCopy[itemCopy.indexOf(item)].visible = true
                continue
            }
            val filteredQuest = itemCopy.filter { (it.options as Quest).questId ==
                requiredQuestId }

            if (filteredQuest[0].userInput in requiredUserInput){
                itemCopy[itemCopy.indexOf(item)].visible = true
            }else{
                itemCopy[itemCopy.indexOf(item)].visible = false
            }
        }
        return itemCopy
    }

    inner class ExclusiveChoiceViewHolder(val binding: CellLongFormItemExclusiveChoiceBinding) :
        ViewHolder(binding.root) {

        fun bind(item: LongFormItem<*>, position: Int) {
            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE
            binding.title.text = item.title
            binding.description.text = item.description
            binding.chipGroup.removeAllViews()
            val quest = item.options as Quest

            quest.questAnswerChoices?.apply {
                for (questItem in this) {
                    val chip = Chip(binding.root.context)
                    // val chip = Chip(ContextThemeWrapper(binding.root.context, R.style.back))
                    chip.text = questItem?.choiceText
                    chip.isCheckable = true
                    chip.setOnClickListener {
                        // Handle chip click here
                        Toast.makeText(
                            binding.root.context,
                            "Clicked: ${chip.text}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    if (items[position].userInput == questItem?.value) {
                        chip.isChecked = true
                    } else {
                        chip.isChecked = false
                    }

                    chip.setOnCheckedChangeListener { _, isChecked ->
                        // setColor(isChecked, chip)
                        val index = givenItems.indexOfFirst { (it.options as Quest).questId == quest.questId }
                        if (isChecked){
                            givenItems[index].userInput = questItem?.value
                        }
                        if (quest.questId in needRefreshIds){
                            items = givenItems
                        }
                    }
                    binding.chipGroup.addView(chip)
                }
            }
        }

        fun setColor(isChecked: Boolean, chip: Chip) {
            val defaultColor = chip.chipBackgroundColor?.defaultColor
            val defaultTextColor = chip.textColors.defaultColor
            if (isChecked) {
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.primary
                    )
                )
                chip.setTextColor(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.button_white
                        )
                    )
                )
            } else {
                chip.chipBackgroundColor = defaultColor?.let { ColorStateList.valueOf(it) }
            }
        }
    }

    class DefaultViewHolder(val binding: CellLongFormItemBinding) : ViewHolder(binding.root) {
        fun bind(item: LongFormItem<*>) {
            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE

            binding.title.text = item.title
            binding.description.text = item.description
        }
    }

    inner class CustomTextWatcher : TextWatcher {
        private var position = 0

        fun updatePosition(position: Int) {
            this.position = position
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val item = items[position]

            val index = givenItems.indexOfFirst { (it.options as Quest).questId == (item.options as Quest).questId }
            givenItems[index].userInput = s.toString()
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }

    inner class InputViewHolder(
        val binding: CellLongFormItemInputBinding,
        val customTextWatcher: CustomTextWatcher
    ) :
        ViewHolder(binding.root) {
        fun bind(item: LongFormItem<*>, position: Int) {

            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE
            binding.title.text = item.title
            binding.description.text = item.description

            binding.input.editText?.removeTextChangedListener(customTextWatcher)
            binding.input.editText?.setText(item.userInput)

            customTextWatcher.updatePosition(position)

            binding.input.editText?.addTextChangedListener(customTextWatcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            ViewType.EXCLUSIVE_CHOICE.value -> {
                val binding = CellLongFormItemExclusiveChoiceBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ExclusiveChoiceViewHolder(binding)
            }

            ViewType.INPUT.value -> {
                val binding = CellLongFormItemInputBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return InputViewHolder(binding, CustomTextWatcher())
            }

            else -> {
                val binding = CellLongFormItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return DefaultViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        val item: LongFormItem<T> = items[position]
        val quest = item.options as Quest

        if (quest.questType == "ExclusiveChoice") {
            return ViewType.EXCLUSIVE_CHOICE.value
        } else if (quest.questType == "Numeric") {
            return ViewType.INPUT.value
        } else {
            return ViewType.DEFAULT.value
        }
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val longFormItem = items[position]

        if (holder is DefaultViewHolder) {
            holder.bind(items[position])
        }
        if (holder is LongFormAdapter<*>.ExclusiveChoiceViewHolder) {
            holder.bind(items[position], position)
        }

        if (holder is LongFormAdapter<*>.InputViewHolder) {
            holder.bind(items[position], position)
        }
    }

    interface OnDataEnteredListener {
        fun onDataEntered(questId: Int, questTag: String? = null, questValue: String? = null)
    }
}
