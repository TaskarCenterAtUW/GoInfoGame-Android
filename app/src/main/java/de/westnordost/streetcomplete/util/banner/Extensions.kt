package de.westnordost.streetcomplete.util.banner

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager


internal fun View.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal val isAtLeastLollipop: Boolean
    get() = true

internal fun Context.getScreenWidth(): Float {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x.toFloat()
}

internal fun Context.getStatusBarHeight(): Float {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result.toFloat()
}
