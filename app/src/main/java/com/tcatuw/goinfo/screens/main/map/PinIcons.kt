package com.tcatuw.goinfo.screens.main.map

import androidx.annotation.DrawableRes
import de.westnordost.osmfeatures.FeatureDictionary
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.util.getNameLabel
import com.tcatuw.goinfo.util.getShortHouseNumber
import com.tcatuw.goinfo.util.ktx.getFeature
import com.tcatuw.goinfo.view.presetIconIndex

@DrawableRes fun getPinIcon(featureDictionary: FeatureDictionary, tags: Map<String, String>): Int? {
    val icon = featureDictionary.getFeature(tags)?.let { presetIconIndex[it.icon] }
    if (icon != null) return icon

    if (getShortHouseNumber(tags) != null && getNameLabel(tags) == null) {
        return R.drawable.ic_none
    }

    return null
}

fun getTitle(tags: Map<String, String>): String? =
    getNameLabel(tags) ?: getShortHouseNumber(tags)
