package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.CellLongFormItemBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemExclusiveChoiceBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemImageGridBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemInputBinding
import de.westnordost.streetcomplete.view.CharSequenceText
import de.westnordost.streetcomplete.view.ImageUrl
import de.westnordost.streetcomplete.view.image_select.ImageSelectAdapter
import de.westnordost.streetcomplete.view.image_select.Item2
import androidx.core.graphics.drawable.toDrawable

class LongFormAdapter<T>(val cameraIntent: () -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    var givenItems = emptyList<LongFormQuest>()
    var needRefreshIds = listOf<Int?>()
    var items: List<LongFormQuest> = emptyList()
        set(value) {
            if (givenItems.isEmpty()) {
                givenItems = value
                needRefreshIds = givenItems.map { it.questAnswerDependency?.questionId }
            }

            field = manageVisibility(value).filter { it.visible }
            notifyDataSetChanged()
        }

    var cellLayoutId = R.layout.cell_long_form_item

    enum class ViewType(val value: Int) {
        EXCLUSIVE_CHOICE(1),
        INPUT(2),
        IMAGE(3),
        DEFAULT(4);

        companion object {
            fun fromInt(value: Int): ViewType? = entries.find { it.value == value }
        }
    }

    private fun manageVisibility(itemCopy: List<LongFormQuest>): List<LongFormQuest> {
        for (quest in itemCopy) {

            val requiredUserInput = quest.questAnswerDependency?.requiredValue
            val requiredQuestId = quest.questAnswerDependency?.questionId


            if (requiredUserInput == null || requiredQuestId == null) {
                itemCopy[itemCopy.indexOf(quest)].visible = true
                continue
            }
            val filteredQuest = itemCopy.filter {
                it.questId ==
                    requiredQuestId
            }

            itemCopy[itemCopy.indexOf(quest)].visible =
                filteredQuest[0].userInput in requiredUserInput
        }

        itemCopy.forEach {
            if (!it.visible) {
                it.selectedIndex = null
            }
        }
        return itemCopy
    }

    inner class ExclusiveChoiceViewHolder(val binding: CellLongFormItemExclusiveChoiceBinding) :
        ViewHolder(binding.root) {
        private var defaultColor: Int? = null
        private var defaultTextColor: Int? = null

        fun bind(item: LongFormQuest, position: Int) {
            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE
            binding.title.text = item.questTitle
            binding.description.text = item.questDescription
            binding.chipGroup.removeAllViews()

            item.questAnswerChoices?.apply {
                for (questItem in this) {
//                    val chip = Chip(binding.root.context)
//                    // val chip = Chip(ContextThemeWrapper(binding.root.context, R.style.back))
//                    chip.text = questItem?.choiceText
//                    defaultColor = chip.chipBackgroundColor?.defaultColor
//                    chip.isCheckable = true
//
//                    if (items[position].userInput == questItem?.value) {
//                        chip.isChecked = true
//                    } else {
//                        chip.isChecked = false
//                    }
//                    setColor(chip.isChecked, chip)
//                    chip.setOnCheckedChangeListener { _, isChecked ->
//                        setColor(isChecked, chip)
//                        val index =
//                            givenItems.indexOfFirst { it.questId == item.questId }
//                        if (isChecked) {
//                            givenItems[index].userInput = questItem?.value
//                        }else{
//                            givenItems[index].userInput = null
//                        }
//                        if (item.questId in needRefreshIds) {
//                            items = givenItems
//                        }
//                    }
//                    binding.chipGroup.addView(chip)

                    binding.chipGroup.addCustomChip(
                        questItem?.choiceText!!,
                        questItem.imageUrl, items[position].userInput == questItem.value
                    ) { isChecked ->
                        val index =
                            givenItems.indexOfFirst { it.questId == item.questId }
                        if (isChecked == true) {
                            givenItems[index].userInput = questItem.value
                        } else {
                            givenItems[index].userInput = null
                        }
                        if (item.questId in needRefreshIds) {
                            items = givenItems
                        }
                    }
                }
            }
        }

        private fun setColor(isChecked: Boolean, chip: Chip) {
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
                chip.setTextColor(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.traffic_black
                        )
                    )
                )
            }
        }
    }

    class DefaultViewHolder(val binding: CellLongFormItemBinding) : ViewHolder(binding.root) {
        fun bind(item: LongFormQuest) {
            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE

            binding.title.text = item.questTitle
            binding.description.text = item.questDescription
        }
    }

    inner class CustomTextWatcher : TextWatcher {
        private var position = 0
        private var textInputLayout: TextInputLayout? = null
        private var minValue: Int? = null
        fun updatePosition(position: Int) {
            this.position = position
        }

        fun updateInputLayout(textInputLayout: TextInputLayout, minValue: Int? = null) {
            this.textInputLayout = textInputLayout
            this.minValue = minValue
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val item = items[position]

            val index =
                givenItems.indexOfFirst { it.questId == item.questId }
            givenItems[index].userInput = s.toString()
        }

        override fun afterTextChanged(s: Editable?) {
            val text = s.toString()
            if (text.isNotBlank() && text.toInt() < minValue!!) {
                textInputLayout?.error = "Value should be greater than $minValue"
            } else {
                textInputLayout?.error = null
            }
        }
    }

    inner class InputViewHolder(
        val binding: CellLongFormItemInputBinding,
        private val customTextWatcher: CustomTextWatcher,
    ) :
        ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                hideKeyboard(it)
            }
        }

        private fun hideKeyboard(view: View) {
            val imm = binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun bind(item: LongFormQuest, position: Int) {

            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE
            binding.title.text = item.questTitle
            binding.description.text = item.questDescription
            binding.input.editText?.clearFocus()
            binding.input.clearFocus()
            binding.input.editText?.removeTextChangedListener(customTextWatcher)
            binding.input.editText?.setText(item.userInput)
            if (!item.questImageUrl.isNullOrBlank()) {
                binding.questImage.visibility = View.VISIBLE
                binding.questImage.load(item.questImageUrl) {
                    placeholder(R.drawable.blank_big)
                    error(R.drawable.blank_big)
                    crossfade(true) // Smooth transition effect
                }

                binding.questImage.setOnLongClickListener {
                    val dialog = Dialog(it.context)
                    dialog.setContentView(R.layout.dialog_full_image)

                    dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    dialog.window?.setDimAmount(0.7f) // controls dim background

                    val fullImageView = dialog.findViewById<PhotoView>(R.id.fullImage)
                    fullImageView.setImageDrawable(binding.questImage.drawable)

                    fullImageView.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                    true
                }

            } else {
                binding.questImage.visibility = View.GONE
            }
            customTextWatcher.updatePosition(position)
            customTextWatcher.updateInputLayout(binding.input, item.questAnswerValidation?.min)
            binding.input.editText?.addTextChangedListener(customTextWatcher)
        }
    }

    inner class ImageGridViewHolder(
        val binding: CellLongFormItemImageGridBinding,
    ) : ViewHolder(binding.root) {
        fun bind(item: LongFormQuest, position: Int) {

            binding.title.text = item.questTitle
            binding.description.text = item.questDescription
            val imageSelectAdapter = ImageSelectAdapter<LongFormQuest>(1)
            binding.list.layoutManager = GridLayoutManager(binding.root.context, 3)
            binding.list.isNestedScrollingEnabled = false
            binding.list.adapter = imageSelectAdapter
            binding.choiceFollowUp.setOnClickListener {
                cameraIntent()
            }
            imageSelectAdapter.selectedIndices = item.selectedIndex?.let { listOf(it) } ?: emptyList()
            imageSelectAdapter.listeners.add(object : ImageSelectAdapter.OnItemSelectionListener {
                override fun onIndexSelected(index: Int) {
                    // checkIsFormComplete()
                    handleClick(
                        item.questId!!,
                        item.questAnswerChoices?.get(index)?.value!!,
                        item.questAnswerChoices,
                        index, binding
                    )
                    if (!item.questAnswerChoices.get(index)?.choiceFollowUp.isNullOrBlank()){
                        binding.choiceFollowUp.visibility = View.VISIBLE
                        binding.choiceFollowUp.text = item.questAnswerChoices[index]?.choiceFollowUp
                    }else{
                        binding.choiceFollowUp.visibility = View.GONE
                    }
                }

                override fun onIndexDeselected(index: Int) {
                    // checkIsFormComplete()
                    val mainIndex =
                        givenItems.indexOfFirst { it.questId == item.questId }
                    givenItems[mainIndex].selectedIndex = null
                    givenItems[mainIndex].userInput = null
                }
            })


            imageSelectAdapter.items = item.questAnswerChoices?.map {
                Item2(item, ImageUrl(it?.imageUrl), CharSequenceText(it?.choiceText!!), CharSequenceText(""))
            }!!

        }

        fun handleClick(
            questId: Int,
            userInput: String,
            questAnswerChoices: List<QuestAnswerChoice?>,
            imageIndex: Int,
            binding: CellLongFormItemImageGridBinding
        ) {
            val index =
                givenItems.indexOfFirst { it.questId == questId }
            givenItems[index].userInput = userInput
            givenItems[index].selectedIndex = imageIndex
            if (questId in needRefreshIds) {
                items = givenItems
            }
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

            ViewType.IMAGE.value -> {
                val binding = CellLongFormItemImageGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ImageGridViewHolder(binding)
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
        val quest = items[position]

        return when (quest.questType) {
            "ExclusiveChoice" -> {
                ViewType.IMAGE.value
            }

            "Numeric" -> {
                ViewType.INPUT.value
            }

            else -> {
                ViewType.DEFAULT.value
            }
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

        if (holder is LongFormAdapter<*>.ImageGridViewHolder) {
            holder.bind(items[position], position)
        }
    }

    interface OnDataEnteredListener {
        fun onDataEntered(questId: Int, questTag: String? = null, questValue: String? = null)
    }
}
