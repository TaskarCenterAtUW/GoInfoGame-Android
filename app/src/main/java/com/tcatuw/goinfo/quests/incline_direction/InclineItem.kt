package com.tcatuw.goinfo.quests.incline_direction

import android.content.Context
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.view.DrawableImage
import com.tcatuw.goinfo.view.ResText
import com.tcatuw.goinfo.view.RotatedCircleDrawable
import com.tcatuw.goinfo.view.image_select.DisplayItem
import com.tcatuw.goinfo.view.image_select.Item2

fun Incline.asItem(context: Context, rotation: Float): DisplayItem<Incline> {
    val drawable = RotatedCircleDrawable(context.getDrawable(iconResId)!!)
    drawable.rotation = rotation
    return Item2(this, DrawableImage(drawable), ResText(R.string.quest_steps_incline_up))
}

private val Incline.iconResId: Int get() = when (this) {
    Incline.UP -> R.drawable.ic_steps_incline_up
    Incline.UP_REVERSED -> R.drawable.ic_steps_incline_up_reversed
}
