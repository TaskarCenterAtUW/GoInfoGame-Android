package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.CellLongFormItemBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemExclusiveChoiceBinding
import de.westnordost.streetcomplete.databinding.CellLongFormItemInputBinding
import de.westnordost.streetcomplete.quests.LongFormItem
import de.westnordost.streetcomplete.util.Listeners

class LongFormAdapter<T> :
    RecyclerView.Adapter<ViewHolder>() {

    var items = listOf<LongFormItem<T>>()
        set(value) {
            field = value.filter { it.visible }
            notifyDataSetChanged()
        }
    var answerMap: MutableMap<Int, Pair<String, String>> = mutableMapOf()
        set(value) {
            field = value
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

    val listeners = Listeners<OnDataEnteredListener>()

    inner class ExclusiveChoiceViewHolder(val binding: CellLongFormItemExclusiveChoiceBinding) :
        ViewHolder(binding.root) {

        fun bind(item: LongFormItem<*>) {
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

                    if (answerMap[quest.questId]?.second == questItem?.value) {
                        chip.isChecked = true
                    } else {
                        chip.isChecked = false
                    }
                    setColor(chip.isChecked, chip)


                    chip.setOnCheckedChangeListener { _, isChecked ->
                        setColor(isChecked, chip)
                        if (isChecked)
                            listeners.forEach {
                                it.onDataEntered(
                                    quest.questId!!,
                                    quest.questTag.toString(),
                                    questItem?.value.toString()
                                )
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

    inner class InputViewHolder(val binding: CellLongFormItemInputBinding) :
        ViewHolder(binding.root) {
        fun bind(item: LongFormItem<*>) {

            if (item.visible) binding.container.visibility =
                View.VISIBLE else binding.container.visibility = View.GONE
            binding.title.text = item.title
            binding.description.text = item.description

            val quest = item.options as Quest



            if (answerMap[quest.questId] != null) {

                binding.input.editText?.setText(answerMap[quest.questId]?.second)
                // Optionally clear focus if it was set during scrolling or other interactions
                binding.input.clearFocus()
                //setTextWithoutCursor(binding.input, answerMap[quest.questId]?.second.toString())
            }


            binding.input.editText?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId in intArrayOf(
                        EditorInfo.IME_ACTION_DONE,
                        EditorInfo.IME_ACTION_GO,
                        EditorInfo.IME_ACTION_NEXT
                    )
                ) {
                    val enteredText = binding.input.editText?.text.toString()
                    listeners.forEach {
                        it.onDataEntered(
                            quest.questId!!,
                            quest.questTag.toString(),
                            enteredText
                        )
                    }
                    true // Consume the event
                } else {
                    false // Let the system handle other actions
                }
            }

            // binding.input.addTextChangedListener(object : TextWatcher{
            //     override fun beforeTextChanged(
            //         s: CharSequence?,
            //         start: Int,
            //         count: Int,
            //         after: Int,
            //     ) {
            //     }
            //
            //     override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //
            //     }
            //
            //     override fun afterTextChanged(s: Editable?) {
            //         searchRunnable?.let { handler.removeCallbacks(it) }
            //
            //         // Define the new search runnable
            //         searchRunnable = Runnable {
            //             listeners.forEach { it.onDataEntered(quest.questId!!, quest.questTag.toString(),s.toString()) }
            //         }
            //
            //         // Post the new search runnable with a delay (e.g., 500ms)
            //         handler.postDelayed(searchRunnable!!, 500)
            //     }
            // })
        }

        private fun setTextWithoutCursor(editText: TextInputEditText, newText: String) {
            // Save the current focus state
            val hadFocus = editText.hasFocus()

            // Temporarily disable focus
            editText.clearFocus()
            editText.isFocusable = false
            editText.isFocusableInTouchMode = false

            // Set the text
            editText.setText(newText)

            // Restore focusability
            editText.isFocusable = true
            editText.isFocusableInTouchMode = true

            // Optionally hide the soft keyboard if it was shown
            if (hadFocus) {
                val imm = binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
            }

            // Clear focus again to ensure no cursor is shown
            editText.clearFocus()
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
                return InputViewHolder(binding)
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
            holder.bind(items[position])
        }

        if (holder is LongFormAdapter<*>.InputViewHolder) {
            holder.bind(items[position])
        }
    }

    interface OnDataEnteredListener {
        fun onDataEntered(questId: Int, questTag: String? = null, questValue: String? = null)
    }
}
