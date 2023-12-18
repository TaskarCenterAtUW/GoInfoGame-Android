package com.tcatuw.goinfo.osm.sidewalk

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.INVALID
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.NO
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.SEPARATE
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.YES
import com.tcatuw.goinfo.view.controller.StreetSideDisplayItem
import com.tcatuw.goinfo.view.controller.StreetSideItem
import com.tcatuw.goinfo.view.image_select.DisplayItem
import com.tcatuw.goinfo.view.image_select.Item

fun Sidewalk.asStreetSideItem(): StreetSideDisplayItem<Sidewalk>? =
    if (this == INVALID) null
    else StreetSideItem(this, imageResId, titleResId, iconResId, floatingIconResId)

fun Sidewalk.asItem(): DisplayItem<Sidewalk>? =
    if (this == INVALID) null
    else Item(this, iconResId, titleResId)

private val Sidewalk.iconResId get() = when (this) {
    YES -> R.drawable.ic_sidewalk_yes
    NO -> R.drawable.ic_bare_road_without_feature
    SEPARATE -> R.drawable.ic_sidewalk_separate
    else -> 0
}

private val Sidewalk.imageResId get() = when (this) {
    YES -> R.drawable.ic_sidewalk_illustration_yes
    NO -> R.drawable.ic_sidewalk_illustration_no
    SEPARATE -> R.drawable.ic_sidewalk_illustration_no
    else -> 0
}

private val Sidewalk.floatingIconResId get() = when (this) {
    SEPARATE -> R.drawable.ic_floating_separate
    else -> null
}

private val Sidewalk.titleResId get() = when (this) {
    YES -> R.string.quest_sidewalk_value_yes
    NO -> R.string.quest_sidewalk_value_no
    SEPARATE -> R.string.quest_sidewalk_value_separate
    else -> 0
}
