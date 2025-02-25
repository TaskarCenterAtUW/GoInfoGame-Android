package de.westnordost.streetcomplete.quests.sidewalk_long_form.data

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import coil.load
import com.google.android.material.chip.Chip
import de.westnordost.streetcomplete.R

class CustomChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val chip: Chip

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_custom_chip, this, true)

        // Get views
        imageView = findViewById(R.id.custom_chip_image)
        chip = findViewById(R.id.custom_chip)

        // Set layout orientation
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // Set chip text
    fun setChipText(text: String) {
        chip.text = text
    }

    fun getBackgroundColor(): Int? {
        return chip.chipBackgroundColor?.defaultColor
    }

    fun getChip() : Chip {
        return chip
    }

    // Set chip checked state
    fun setChecked(checked: Boolean) {
        chip.isChecked = checked
    }

    // Set image from URL using Glide
    fun setImage(url: String?) {
        if (!url.isNullOrEmpty()) {
            imageView.load(url) {
                placeholder(R.drawable.surface_asphalt)
                error(R.drawable.surface_dirt)
                crossfade(true) // Smooth transition effect
            }
            imageView.isVisible = true
        } else {
            imageView.isVisible = false
        }
    }

    // Get chip checked state
    fun isChecked(): Boolean = chip.isChecked

    // Set click listener for chip
    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
        chip.setOnCheckedChangeListener(listener)
    }
}

