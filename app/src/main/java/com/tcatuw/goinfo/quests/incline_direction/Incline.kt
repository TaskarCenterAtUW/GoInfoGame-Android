package com.tcatuw.goinfo.quests.incline_direction

import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.incline_direction.Incline.*

enum class Incline {
    UP, UP_REVERSED
}

fun Incline.applyTo(tags: Tags) {
    tags["incline"] = when (this) {
        UP -> "up"
        UP_REVERSED -> "down"
    }
}
