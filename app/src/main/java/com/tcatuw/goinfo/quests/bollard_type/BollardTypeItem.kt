package com.tcatuw.goinfo.quests.bollard_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.bollard_type.BollardType.FIXED
import com.tcatuw.goinfo.quests.bollard_type.BollardType.FLEXIBLE
import com.tcatuw.goinfo.quests.bollard_type.BollardType.FOLDABLE
import com.tcatuw.goinfo.quests.bollard_type.BollardType.REMOVABLE
import com.tcatuw.goinfo.quests.bollard_type.BollardType.RISING
import com.tcatuw.goinfo.view.image_select.Item

fun BollardType.asItem() = Item(this, iconResId, titleResId)

private val BollardType.titleResId: Int get() = when (this) {
    RISING ->    R.string.quest_bollard_type_rising
    REMOVABLE -> R.string.quest_bollard_type_removable
    FOLDABLE ->  R.string.quest_bollard_type_foldable2
    FLEXIBLE ->  R.string.quest_bollard_type_flexible
    FIXED ->     R.string.quest_bollard_type_fixed
}

private val BollardType.iconResId: Int get() = when (this) {
    RISING ->    R.drawable.bollard_rising
    REMOVABLE -> R.drawable.bollard_removable
    FOLDABLE ->  R.drawable.bollard_foldable
    FLEXIBLE ->  R.drawable.bollard_flexible
    FIXED ->     R.drawable.bollard_fixed
}
