package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.chip.Chip
import de.westnordost.streetcomplete.R

class CustomChipGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FlexboxLayout(context, attrs, defStyleAttr) {

    private var defaultColor : Int?= null

    init {
        flexWrap = FlexWrap.WRAP
        justifyContent = JustifyContent.FLEX_START
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // Add a custom chip
    fun addCustomChip(text: String, imageUrl: String?, isChecked: Boolean, onChipSelected: (Boolean?) -> Unit) {
        val chipView = CustomChipView(context)
        chipView.setChipText(text)
        chipView.setImage(imageUrl)
        chipView.setChecked(isChecked)
        defaultColor = chipView.getBackgroundColor()
        setColor(isChecked, chipView.getChip())
        // Handle chip selection
        chipView.setOnCheckedChangeListener { view, isChecked ->
            hideKeyboard(view)
            if (isChecked) {
                uncheckOthers(chipView)
            }
            onChipSelected(isChecked) // Return selected chip value
            setColor(isChecked, chipView.getChip())
        }

        chipView.setOnFocusChangeListener { v, hasFocus ->
        }

        addView(chipView)
    }

    private fun hideKeyboard(view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setColor(isChecked: Boolean, chip: Chip) {
        if (isChecked) {
            chip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.primary
                )
            )
            chip.setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.button_white
                    )
                )
            )
        } else {
            chip.chipBackgroundColor = defaultColor?.let { ColorStateList.valueOf(it) }
            chip.setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.traffic_black
                    )
                )
            )
        }
    }

    // Uncheck all other chips except the selected one
    private fun uncheckOthers(selectedChip: CustomChipView) {
        for (i in 0 until childCount) {
            val chipView = getChildAt(i) as? CustomChipView
            if (chipView != selectedChip) {
                chipView?.setChecked(false)
            }
        }
    }
}


