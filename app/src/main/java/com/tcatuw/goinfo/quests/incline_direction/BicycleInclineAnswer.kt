package com.tcatuw.goinfo.quests.incline_direction

import com.tcatuw.goinfo.osm.Tags

sealed interface BicycleInclineAnswer
class RegularBicycleInclineAnswer(val value: Incline) : BicycleInclineAnswer
object UpdAndDownHopsAnswer : BicycleInclineAnswer

fun BicycleInclineAnswer.applyTo(tags: Tags) {
    when (this) {
        is RegularBicycleInclineAnswer -> value.applyTo(tags)
        is UpdAndDownHopsAnswer -> tags["incline"] = "up/down"
    }
}
