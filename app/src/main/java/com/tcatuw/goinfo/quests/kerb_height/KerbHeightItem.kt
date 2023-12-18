package com.tcatuw.goinfo.quests.kerb_height

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight.FLUSH
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight.KERB_RAMP
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight.LOWERED
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight.NO_KERB
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight.RAISED
import com.tcatuw.goinfo.view.image_select.Item

fun KerbHeight.asItem() = Item(this, iconResId, titleResId)

private val KerbHeight.titleResId: Int get() = when (this) {
    RAISED ->    R.string.quest_kerb_height_raised
    LOWERED ->   R.string.quest_kerb_height_lowered
    FLUSH ->     R.string.quest_kerb_height_flush
    KERB_RAMP -> R.string.quest_kerb_height_lowered_ramp
    NO_KERB ->   R.string.quest_kerb_height_no
}

private val KerbHeight.iconResId: Int get() = when (this) {
    RAISED ->    R.drawable.kerb_height_raised
    LOWERED ->   R.drawable.kerb_height_lowered
    FLUSH ->     R.drawable.kerb_height_flush
    KERB_RAMP -> R.drawable.kerb_height_lowered_ramp
    NO_KERB ->   R.drawable.kerb_height_no
}
