package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.google.android.material.textfield.TextInputLayout
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.CellLongFormItemBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemImageGridBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemInputBinding
import de.westnordost.streetcomplete.view.CharSequenceText
import de.westnordost.streetcomplete.view.ImageUrl
import de.westnordost.streetcomplete.view.image_select.ImageSelectAdapter
import de.westnordost.streetcomplete.view.image_select.Item2

class LongFormAdapter<T>(val cameraIntent: () -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    var givenItems = emptyList<LongFormQuest>()
    var needRefreshIds = listOf<Int?>()
    var items: List<LongFormQuest> = emptyList()
        set(value) {
            if (givenItems.isEmpty()) {
                givenItems = value
                needRefreshIds = givenItems.map { dependency ->
                    when (val dep = dependency.questAnswerDependency) {
                        is List<*> -> dep.mapNotNull { it.questionId }
                        else -> emptyList()
                    }
                }.flatten()
            }

            field = manageVisibility(value).filter { it.visible }
            notifyDataSetChanged()
        }

    var cellLayoutId = R.layout.cell_long_form_item

    enum class ViewType(val value: Int) {
        EXCLUSIVE(1),
        NUMERIC(2),
        MULTI_CHOICE(3),
        TextEntry(4),
        DEFAULT(5);

        companion object {
            fun fromInt(value: Int): ViewType? = entries.find { it.value == value }
        }
    }

    private fun manageVisibility(itemCopy: List<LongFormQuest>): List<LongFormQuest> {
        for (quest in itemCopy) {
            val dependencies = quest.questAnswerDependency ?: emptyList()
            var isVisible = true

            for (dependency in dependencies) {
                val requiredUserInput = dependency.requiredValue
                val requiredQuestId = dependency.questionId

                if (requiredUserInput == null || requiredQuestId == null) continue

                val filteredQuest = itemCopy.find { it.questId == requiredQuestId }
                if (filteredQuest != null) {
                    when (filteredQuest.userInput) {
                        is UserInput.Single -> {
                            if ((filteredQuest.userInput as UserInput.Single).answer !in requiredUserInput) {
                                isVisible = false
                                break
                            }
                        }

                        is UserInput.Multiple -> {
                            val userInputs = (filteredQuest.userInput as UserInput.Multiple).answers
                            if (userInputs.none { it in requiredUserInput }) {
                                isVisible = false
                                break
                            }
                        }

                        else -> {
                            isVisible = false
                            break
                        }
                    }
                }
            }
            itemCopy[itemCopy.indexOf(quest)].visible = isVisible
        }

        itemCopy.forEach {
            if (!it.visible) {
                it.selectedIndex = null
            }
        }
        return itemCopy
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
        private var maxValue: Int = Int.MAX_VALUE
        fun updatePosition(position: Int) {
            this.position = position
        }

        fun updateInputLayout(
            textInputLayout: TextInputLayout,
            minValue: Int? = null,
            maxValue: Int?
        ) {
            this.textInputLayout = textInputLayout
            this.minValue = minValue
            this.maxValue = maxValue ?: Int.MAX_VALUE
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val item = items[position]

            val index =
                givenItems.indexOfFirst { it.questId == item.questId }
            givenItems[index].userInput = UserInput.Single(s.toString())
        }

        override fun afterTextChanged(s: Editable?) {
            val text = s.toString()
            if (text.isNotBlank()) {
                val number = text.toLongOrNull()
                if (number == null) {
                    textInputLayout?.error = "Invalid number"
                } else if (number < (minValue?.toLong() ?: 0L)) {
                    textInputLayout?.error = "Value should be greater than $minValue"
                } else if (number > maxValue.toLong()) {
                    textInputLayout?.error = "Value should be less than $maxValue"
                } else {
                    textInputLayout?.error = null
                }
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
            val imm =
                binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            binding.input.editText?.setText((item.userInput as? UserInput.Single)?.answer ?: "")
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

                    val fullImageView = dialog.findViewById<ImageView>(R.id.fullImage)
                    val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
                    closeButton.setOnClickListener {
                        dialog.dismiss()
                    }
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
            customTextWatcher.updateInputLayout(
                binding.input,
                item.questAnswerValidation?.min,
                item.questAnswerValidation?.max
            )
            binding.input.editText?.addTextChangedListener(customTextWatcher)
        }
    }

    inner class ImageGridViewHolder(
        val binding: CellLongFormItemImageGridBinding,
        val allowMultiChoice: Boolean,
    ) : ViewHolder(binding.root) {
        fun bind(item: LongFormQuest, position: Int) {

            binding.title.text = item.questTitle
            binding.description.text = item.questDescription
            val imageSelectAdapter = ImageSelectAdapter<LongFormQuest>()
            binding.list.layoutManager = GridLayoutManager(binding.root.context, 3)
            binding.list.isNestedScrollingEnabled = false
            binding.list.adapter = imageSelectAdapter
            binding.choiceFollowUp.setOnClickListener {
                cameraIntent()
            }
            if (allowMultiChoice)
                item.userInput = UserInput.Multiple(mutableListOf())
            else if (item.userInput == null)
                imageSelectAdapter.selectedIndices =
                    item.selectedIndex ?: emptyList()
            imageSelectAdapter.listeners.add(object : ImageSelectAdapter.OnItemSelectionListener {
                override fun onIndexSelected(index: Int) {
                    // checkIsFormComplete()
                    handleClick(
                        item.questId!!,
                        item.questAnswerChoices?.get(index)?.value!!,
                        item.questAnswerChoices,
                        index, binding
                    )
                    if (!item.questAnswerChoices.get(index)?.choiceFollowUp.isNullOrBlank()) {
                        binding.choiceFollowUp.visibility = View.VISIBLE
                        binding.choiceFollowUp.text = item.questAnswerChoices[index]?.choiceFollowUp
                    } else {
                        binding.choiceFollowUp.visibility = View.GONE
                    }
                }

                override fun onIndexDeselected(index: Int) {
                    // checkIsFormComplete()
                    val mainIndex =
                        givenItems.indexOfFirst { it.questId == item.questId }
                    givenItems[mainIndex].selectedIndex?.remove(index)
                    givenItems[mainIndex].userInput = null
                }

                override fun onLongPress(index: Int, drawable: Drawable?) {

                    val mainIndex =
                        givenItems.indexOfFirst { it.questId == item.questId }

                    val dialog = Dialog(binding.root.context)
                    dialog.setContentView(R.layout.dialog_full_image)

                    dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    dialog.window?.setDimAmount(0.7f) // controls dim background

                    val fullImageView = dialog.findViewById<ImageView>(R.id.fullImage)
                    val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
                    val title = dialog.findViewById<TextView>(R.id.title)
                    val description = dialog.findViewById<TextView>(R.id.description)
                    val choice_name = dialog.findViewById<TextView>(R.id.choice_name)

                    title.text = givenItems[mainIndex].questTitle
                    description.text = givenItems[mainIndex].questDescription
                    choice_name.text = item.questAnswerChoices?.get(index)?.choiceText

                    closeButton.setOnClickListener {
                        dialog.dismiss()
                    }
                    fullImageView.setImageDrawable(drawable)
                    fullImageView.contentDescription = item.questAnswerChoices?.get(index)?.value
                    fullImageView.setOnClickListener {
                        dialog.dismiss()
                    }

                    ViewCompat.replaceAccessibilityAction(
                        fullImageView,
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            "close"
                        ), "close"
                    ) { _, _ ->
                        fullImageView.performClick()
                        true
                    }

                    dialog.show()
                }
            })


            imageSelectAdapter.items = item.questAnswerChoices?.map {
                Item2(
                    item,
                    ImageUrl(it?.imageUrl),
                    CharSequenceText(it?.choiceText!!),
                    CharSequenceText("")
                )
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
            if (givenItems[index].userInput is UserInput.Multiple) {
                val multiple = givenItems[index].userInput as UserInput.Multiple
                multiple.answers.add(userInput)
                givenItems[index].userInput = multiple
            } else {
                givenItems[index].userInput = UserInput.Single(userInput)
            }
            if (givenItems[index].selectedIndex == null) {
                givenItems[index].selectedIndex = mutableListOf(imageIndex)
            } else {
                givenItems[index].selectedIndex?.add(imageIndex)
            }
            if (questId in needRefreshIds) {
                items = givenItems
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            ViewType.NUMERIC.value -> {
                val binding = CellLongFormItemInputBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return InputViewHolder(binding, CustomTextWatcher())
            }

            ViewType.EXCLUSIVE.value -> {
                val binding = CellLongFormItemImageGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ImageGridViewHolder(binding, true)
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
                ViewType.EXCLUSIVE.value
            }

            "Numeric" -> {
                ViewType.NUMERIC.value
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
