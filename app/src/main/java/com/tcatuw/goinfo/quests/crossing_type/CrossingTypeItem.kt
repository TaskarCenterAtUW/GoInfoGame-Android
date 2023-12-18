package com.tcatuw.goinfo.quests.crossing_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.quests.crossing_type.CrossingType.MARKED
import com.tcatuw.goinfo.quests.crossing_type.CrossingType.TRAFFIC_SIGNALS
import com.tcatuw.goinfo.quests.crossing_type.CrossingType.UNMARKED
import com.tcatuw.goinfo.view.image_select.Item

fun CrossingType.asItem() = Item(this, iconResId, titleResId)

private val CrossingType.titleResId: Int get() = when (this) {
    TRAFFIC_SIGNALS -> R.string.quest_crossing_type_signals_controlled
    MARKED ->          R.string.quest_crossing_type_marked
    UNMARKED ->        R.string.quest_crossing_type_unmarked
}

private val CrossingType.iconResId: Int get() = when (this) {
    TRAFFIC_SIGNALS -> R.drawable.crossing_type_signals
    MARKED ->          R.drawable.crossing_type_zebra
    UNMARKED ->        R.drawable.crossing_type_unmarked
}
