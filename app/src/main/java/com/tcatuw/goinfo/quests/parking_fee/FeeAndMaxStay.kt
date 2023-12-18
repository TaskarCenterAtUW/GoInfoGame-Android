package com.tcatuw.goinfo.quests.parking_fee

import com.tcatuw.goinfo.osm.Tags

data class FeeAndMaxStay(val fee: Fee, val maxstay: Maxstay? = null)

fun FeeAndMaxStay.applyTo(tags: Tags) {
    fee.applyTo(tags)
    maxstay?.applyTo(tags)
}
