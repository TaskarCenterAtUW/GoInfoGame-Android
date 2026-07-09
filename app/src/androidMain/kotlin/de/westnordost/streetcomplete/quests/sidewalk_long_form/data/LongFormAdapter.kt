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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.textfield.TextInputLayout
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.databinding.CellLongFormItemBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemImageGridBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemInputBinding
import de.westnordost.streetcomplete.databinding.CellLongFormTextEntryItemBinding
import de.westnordost.streetcomplete.view.CharSequenceText
import de.westnordost.streetcomplete.view.ImageUrl
import de.westnordost.streetcomplete.view.image_select.ImageSelectAdapter
import de.westnordost.streetcomplete.view.image_select.Item2
import de.westnordost.streetcomplete.view.setImage
import org.koin.java.KoinJavaComponent.inject

class LongFormAdapter<T>(val cameraIntent: () -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {
    var givenItems = emptyList<LongFormQuest>()
    var needRefreshIds = listOf<Int?>()
    val preferences: Preferences by inject(Preferences::class.java)
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

            // snapshot each item rather than keeping the live (mutated-in-place) instances, so the
            // diff below compares genuinely independent before/after values - see LongFormQuest.snapshot()
            val newList = manageVisibility(value).filter { it.visible }.map { it.snapshot() }
            val diff = DiffUtil.calculateDiff(LongFormQuestDiffCallback(field, newList))
            field = newList
            diff.dispatchUpdatesTo(this)
        }

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
        val byQuestId = itemCopy.associateBy { it.questId }

        for (quest in itemCopy) {
            val dependencies = quest.questAnswerDependency ?: emptyList()
            var isVisible = true

            for (dependency in dependencies) {
                val requiredUserInput = dependency.requiredValue
                val requiredQuestId = dependency.questionId

                if (requiredUserInput == null || requiredQuestId == null) continue

                val filteredQuest = byQuestId[requiredQuestId]
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
            quest.visible = isVisible
        }

        itemCopy.forEach {
            if (!it.visible) {
                it.selectedIndex = null
            }
        }
        return itemCopy
    }

    /** Diffs two snapshots of the visible question list so only rows that actually appeared,
     *  disappeared, moved or changed content get rebound - not the whole list every time. */
    private class LongFormQuestDiffCallback(
        private val oldList: List<LongFormQuest>,
        private val newList: List<LongFormQuest>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].questId == newList[newItemPosition].questId

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
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
            maxValue: Int?,
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
                val number = text.toFloatOrNull()
                if (number == null) {
                    textInputLayout?.error = "Invalid number"
                } else if (number < (minValue?.toFloat() ?: 0F)) {
                    textInputLayout?.error = "Value should be greater than $minValue"
                } else if (number > maxValue.toFloat()) {
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
            if (!item.questImageUrl.isNullOrBlank() && !preferences.isLowBandwidthModeEnabled) {
                binding.questImage.visibility = View.VISIBLE

                binding.questImage.setImage(
                    image =
                        ImageUrl(item.questImageUrl),
                    progressBar = binding.progressBar
                )

                binding.questImage.setOnLongClickListener {
                    val dialog = Dialog(binding.root.context)
                    showDialog(dialog, binding.questImage.drawable, item)
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

    inner class TextEntryViewHolder(
        val binding: CellLongFormTextEntryItemBinding,
    ) : ViewHolder(binding.root) {

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
            binding.input.editText?.setText((item.userInput as? UserInput.Single)?.answer ?: "")
            if (!item.questImageUrl.isNullOrBlank() && !preferences.isLowBandwidthModeEnabled) {
                binding.questImage.visibility = View.VISIBLE
                binding.questImage.setImage(
                    image =
                        ImageUrl(item.questImageUrl),
                    progressBar = binding.progressBar
                )

                binding.questImage.setOnLongClickListener {
                    val dialog = Dialog(binding.root.context)
                    showDialog(dialog, binding.questImage.drawable, item)
                }
            } else {
                binding.questImage.visibility = View.GONE
            }
            binding.input.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.userInput = UserInput.Single(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun showDialog(
        dialog: Dialog,
        drawable: Drawable,
        item: LongFormQuest,
    ): Boolean {
        dialog.setContentView(R.layout.dialog_full_image)

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.setDimAmount(0.7f) // controls dim background

        val fullImageView = dialog.findViewById<ImageView>(R.id.fullImage)
        val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
        val title = dialog.findViewById<TextView>(R.id.title)
        val description = dialog.findViewById<TextView>(R.id.description)

        title.text = item.questTitle
        description.text = item.questDescription

        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        fullImageView.setImageDrawable(drawable)
        fullImageView.contentDescription = item.questTitle
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
        return true
    }

    inner class ImageGridViewHolder(
        val binding: CellLongFormItemImageGridBinding,
        val allowMultiChoice: Boolean,
    ) : ViewHolder(binding.root) {

        // Created once and reused across rebinds. Replacing binding.list.adapter on every bind
        // (which happens whenever this question is in needRefreshIds and any answer changes)
        // forced the inner RecyclerView to recycle its ImageViews into a brand new adapter
        // instance, which could let an in-flight image load for the old item land on the
        // recycled view after it was rebound to a different item.
        private val imageSelectAdapter =
            ImageSelectAdapter<LongFormQuest>(if (allowMultiChoice) -1 else 1)
        private var selectionListener: ImageSelectAdapter.OnItemSelectionListener? = null

        // the choices for a given question never change once loaded, so this lets bind() skip
        // rebuilding imageSelectAdapter.items (and the image reloads that would trigger) when this
        // row was rebound for a reason unrelated to its own choices, e.g. another question's answer
        // changing this row's position or an unrelated selection elsewhere in the form
        private var boundQuestId: Int? = null

        init {
            binding.list.layoutManager = GridLayoutManager(binding.root.context, 3)
            binding.list.isNestedScrollingEnabled = false
            binding.list.adapter = imageSelectAdapter
        }

        fun bind(item: LongFormQuest, position: Int) {

            binding.title.text = item.questTitle
            binding.title.contentDescription = if (allowMultiChoice) {
                "${item.questTitle}. Multiple items can be selected"
            } else {
                "${item.questTitle}. Only one item can be selected"
            }
            if (!item.questImageUrl.isNullOrBlank() && !preferences.isLowBandwidthModeEnabled) {
                binding.imageView.setImage(
                    ImageUrl(item.questImageUrl),
                    progressBar = binding.progressBar
                )
                binding.imageView.visibility = View.VISIBLE
            } else {
                binding.imageView.visibility = View.GONE
            }

            binding.imageView.setOnLongClickListener {
                val dialog = Dialog(binding.root.context)
                showDialog(dialog, binding.imageView.drawable, item)
            }

            binding.description.text = item.questDescription
            binding.choiceFollowUp.setOnClickListener {
                cameraIntent()
            }
            // apply from current state, not just on selection events - the follow-up must also
            // survive rebinds and holders recycled from other questions
            updateChoiceFollowUp(item)

            imageSelectAdapter.selectedIndices =
                item.selectedIndex ?: emptyList()

            selectionListener?.let { imageSelectAdapter.listeners.remove(it) }
            val listener = object : ImageSelectAdapter.OnItemSelectionListener {
                override fun onIndexSelected(index: Int) {
                    // checkIsFormComplete()
                    handleSelection(
                        item.questId!!,
                        item.questAnswerChoices?.get(index)?.value!!,
                        index
                    )
                    handleChoiceFollowUp()
                }

                override fun onIndexDeselected(index: Int) {
                    // checkIsFormComplete()
                    handleDeselection(
                        item.questId!!,
                        item.questAnswerChoices?.get(index)?.value!!,
                        index
                    )
                    handleChoiceFollowUp()
                }

                fun handleChoiceFollowUp() {
                    // item is a snapshot taken at bind time - the selection that was just made
                    // lives in givenItems, so read the follow-up state from there
                    val live = givenItems.firstOrNull { it.questId == item.questId } ?: item
                    updateChoiceFollowUp(live)
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
            }
            imageSelectAdapter.listeners.add(listener)
            selectionListener = listener

            if (item.questId != boundQuestId) {
                boundQuestId = item.questId
                imageSelectAdapter.items = item.questAnswerChoices?.map {
                    Item2(
                        item,
                        ImageUrl(it?.imageUrl),
                        CharSequenceText(it?.choiceText!!),
                        CharSequenceText("")
                    )
                }!!
            } else {
                // choices are unchanged, so the items setter above (and its own notifyDataSetChanged)
                // was skipped - refresh explicitly so the selected/deselected highlight still updates
                imageSelectAdapter.notifyDataSetChanged()
            }
        }

        /** Show the follow-up prompt (e.g. "Please take a photo of the obstruction.") of the
         *  first selected choice that has one, hide it if none of the selected choices do */
        private fun updateChoiceFollowUp(quest: LongFormQuest) {
            quest.selectedIndex?.forEach { index ->
                val followUp = quest.questAnswerChoices?.get(index)?.choiceFollowUp
                if (!followUp.isNullOrBlank()) {
                    binding.choiceFollowUp.visibility = View.VISIBLE
                    binding.choiceFollowUp.text = followUp
                    return
                }
            }
            binding.choiceFollowUp.visibility = View.GONE
        }

        fun handleDeselection(
            questId: Int,
            userInput: String,
            imageIndex: Int,
        ) {
            val index =
                givenItems.indexOfFirst { it.questId == questId }
            if (allowMultiChoice) {
                val multiple = givenItems[index].userInput as? UserInput.Multiple

                multiple?.let {
                    if (!it.isEmpty()) {
                        multiple.answers.remove(userInput)
                    }
                }
                givenItems[index].userInput = multiple
            } else {
                givenItems[index].userInput = null
            }
            givenItems[index].selectedIndex?.remove(imageIndex)
            if (questId in needRefreshIds) {
                items = givenItems
            }
        }

        fun handleSelection(
            questId: Int,
            userInput: String,
            imageIndex: Int,
        ) {
            val index =
                givenItems.indexOfFirst { it.questId == questId }
            if (allowMultiChoice) {
                var multiple = givenItems[index].userInput as? UserInput.Multiple
                if (multiple == null) {
                    multiple = UserInput.Multiple(mutableListOf(userInput))
                } else {
                    multiple.answers.add(userInput)
                }
                givenItems[index].userInput = multiple
            } else {
                var single = givenItems[index].userInput as? UserInput.Single
                if (single == null) {
                    single = UserInput.Single(userInput)
                } else {
                    single.answer = userInput
                }
                givenItems[index].userInput = single
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
                return ImageGridViewHolder(binding, false)
            }

            ViewType.MULTI_CHOICE.value -> {
                val binding = CellLongFormItemImageGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ImageGridViewHolder(binding, true)
            }

            ViewType.TextEntry.value -> {
                val binding = CellLongFormTextEntryItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TextEntryViewHolder(binding)
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

            "MultipleChoice" -> {
                ViewType.MULTI_CHOICE.value
            }

            "Numeric" -> {
                ViewType.NUMERIC.value
            }

            "TextEntry" -> {
                ViewType.TextEntry.value
            }

            else -> {
                ViewType.DEFAULT.value
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is DefaultViewHolder) {
            holder.bind(items[position])
        }

        if (holder is LongFormAdapter<*>.InputViewHolder) {
            holder.bind(items[position], position)
        }

        if (holder is LongFormAdapter<*>.ImageGridViewHolder) {
            holder.bind(items[position], position)
        }

        if (holder is LongFormAdapter<*>.TextEntryViewHolder) {
            holder.bind(items[position], position)
        }
    }
}
