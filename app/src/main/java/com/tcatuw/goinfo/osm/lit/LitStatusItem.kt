package com.tcatuw.goinfo.osm.lit

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.osm.lit.LitStatus.AUTOMATIC
import com.tcatuw.goinfo.osm.lit.LitStatus.NIGHT_AND_DAY
import com.tcatuw.goinfo.osm.lit.LitStatus.NO
import com.tcatuw.goinfo.osm.lit.LitStatus.UNSUPPORTED
import com.tcatuw.goinfo.osm.lit.LitStatus.YES
import com.tcatuw.goinfo.view.image_select.DisplayItem
import com.tcatuw.goinfo.view.image_select.Item

fun LitStatus.asItem(): DisplayItem<LitStatus> =
    Item(this, iconResId, titleResId)

private val LitStatus.iconResId: Int get() = when (this) {
    YES -> R.drawable.ic_lit_yes
    NO -> R.drawable.ic_lit_no
    AUTOMATIC -> R.drawable.ic_lit_automatic
    NIGHT_AND_DAY -> R.drawable.ic_lit_24_7
    UNSUPPORTED -> 0
}

private val LitStatus.titleResId: Int get() = when (this) {
    YES -> R.string.lit_value_yes
    NO -> R.string.lit_value_no
    AUTOMATIC -> R.string.lit_value_automatic
    NIGHT_AND_DAY -> R.string.lit_value_24_7
    UNSUPPORTED -> 0
}
