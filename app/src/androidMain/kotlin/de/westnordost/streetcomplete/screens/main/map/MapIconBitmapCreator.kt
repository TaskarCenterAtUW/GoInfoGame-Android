package de.westnordost.streetcomplete.screens.main.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.graphics.toRect
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.util.ktx.createBitmap
import de.westnordost.streetcomplete.util.ktx.dpToPx
import de.westnordost.streetcomplete.util.ktx.toSdf
import kotlin.math.ceil

fun createPinBitmap(
    context: Context,
    @DrawableRes iconResId: Int,
    @DrawableRes tickMarkResId: Int? = null
): Bitmap = createPinBitmap(
    context,
    icon = context.getDrawable(iconResId)!!,
    /* quest icons (ic_quest_*) have padding built into their viewport, but the preset_* feature
       icons are full-bleed glyphs - drawn at full slot size they poke out of the pin bubble,
       so they get an inset instead */
    insetIcon = context.resources.getResourceEntryName(iconResId).startsWith("preset_"),
    tickMarkResId = tickMarkResId
)

fun createPinBitmap(
    context: Context,
    icon: Drawable,
    insetIcon: Boolean,
    @DrawableRes tickMarkResId: Int? = null
): Bitmap {
    val scale = 1f
    val size = context.resources.dpToPx(71 * scale)
    val sizeInt = ceil(size).toInt()
    val iconSize = context.resources.dpToPx(48 * scale)
    val iconPinOffset = context.resources.dpToPx(2 * scale)
    val pinTopRightPadding = context.resources.dpToPx(5 * scale)

    val pin = context.getDrawable(R.drawable.pin)!!
    val pinShadow = context.getDrawable(R.drawable.pin_shadow)!!

    val pinWidth = (size - pinTopRightPadding) * pin.intrinsicWidth / pin.intrinsicHeight
    val pinXOffset = size - pinTopRightPadding - pinWidth

    val bitmap = Bitmap.createBitmap(sizeInt, sizeInt, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    pinShadow.setBounds(0, 0, sizeInt, sizeInt)
    pinShadow.draw(canvas)
    pin.bounds = RectF(
        pinXOffset,
        pinTopRightPadding,
        size - pinTopRightPadding,
        size
    ).toRect()
    pin.draw(canvas)
    val questIcon = icon
    val iconInset = if (insetIcon) iconSize * 0.22f else 0f
    questIcon.bounds = RectF(
        pinXOffset + iconPinOffset + iconInset,
        pinTopRightPadding + iconPinOffset + iconInset,
        pinXOffset + iconPinOffset + iconSize - iconInset,
        pinTopRightPadding + iconPinOffset + iconSize - iconInset
    ).toRect()
    questIcon.draw(canvas)


    // Draw tick mark if provided
    tickMarkResId?.let {
        val tickMark = context.getDrawable(it)!!
        // Position tick mark at top-right of the pin
        val tickSize = context.resources.dpToPx(24 * scale)
        val tickLeft = size - tickSize - pinTopRightPadding
        val tickTop = pinTopRightPadding
        tickMark.bounds = RectF(
            tickLeft,
            tickTop,
            tickLeft + tickSize,
            tickTop + tickSize
        ).toRect()
        tickMark.draw(canvas)
    }

    return bitmap
}

fun createIconBitmap(
    context: Context,
    @DrawableRes iconResId: Int,
    createSdf: Boolean = false,
    maxSizeDp: Int = 48
): Bitmap {
    val drawable = context.getDrawable(iconResId)!!
    val maxIconSize = context.resources.dpToPx(maxSizeDp).toInt()
    val bitmap = drawable.createBitmap(
        width = drawable.intrinsicWidth.coerceAtMost(maxIconSize),
        height = drawable.intrinsicHeight.coerceAtMost(maxIconSize),
    )
    if (!createSdf) return bitmap
    return bitmap.toSdf(radius = context.resources.dpToPx(8.0).toDouble())
}
