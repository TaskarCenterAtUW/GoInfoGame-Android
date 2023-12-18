package com.tcatuw.goinfo.osm.sidewalk_surface

import com.tcatuw.goinfo.osm.expandSidesTags
import com.tcatuw.goinfo.osm.surface.createSurfaceAndNote

fun createSidewalkSurface(tags: Map<String, String>): LeftAndRightSidewalkSurface? {
    val expandedTags = expandRelevantSidesTags(tags)

    val left = createSurfaceAndNote(expandedTags, "sidewalk:left")
    val right = createSurfaceAndNote(expandedTags, "sidewalk:right")

    if (left == null && right == null) return null

    return LeftAndRightSidewalkSurface(left, right)
}

private fun expandRelevantSidesTags(tags: Map<String, String>): Map<String, String> {
    val result = tags.toMutableMap()
    result.expandSidesTags("sidewalk", "surface", true)
    result.expandSidesTags("sidewalk", "surface:note", true)
    return result
}
