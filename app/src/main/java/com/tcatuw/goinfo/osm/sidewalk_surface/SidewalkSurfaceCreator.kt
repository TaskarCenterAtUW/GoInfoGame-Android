package com.tcatuw.goinfo.osm.sidewalk_surface

import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.expandSides
import com.tcatuw.goinfo.osm.hasCheckDateForKey
import com.tcatuw.goinfo.osm.mergeSides
import com.tcatuw.goinfo.osm.surface.applyTo
import com.tcatuw.goinfo.osm.updateCheckDateForKey

fun LeftAndRightSidewalkSurface.applyTo(tags: Tags) {
    tags.expandSides("sidewalk", "surface")
    tags.expandSides("sidewalk", "surface:note")
    tags.expandSides("sidewalk", "smoothness")

    left?.applyTo(tags, "sidewalk:left", updateCheckDate = false)
    right?.applyTo(tags, "sidewalk:right", updateCheckDate = false)

    tags.mergeSides("sidewalk", "surface")
    tags.mergeSides("sidewalk", "surface:note")
    tags.mergeSides("sidewalk", "smoothness")

    if (!tags.hasChanges || tags.hasCheckDateForKey("sidewalk:surface")) {
        tags.updateCheckDateForKey("sidewalk:surface")
    }
}
