package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
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
        chipView.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOthers(chipView)
            }
            onChipSelected(isChecked) // Return selected chip value
            setColor(isChecked, chipView.getChip())
        }

        addView(chipView)
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


