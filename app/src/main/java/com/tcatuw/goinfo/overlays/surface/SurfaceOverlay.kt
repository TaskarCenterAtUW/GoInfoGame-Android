package com.tcatuw.goinfo.overlays.surface

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.*
import com.tcatuw.goinfo.osm.ALL_PATHS
import com.tcatuw.goinfo.osm.ALL_ROADS
import com.tcatuw.goinfo.osm.surface.createSurfaceAndNote
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PolygonStyle
import com.tcatuw.goinfo.overlays.PolylineStyle
import com.tcatuw.goinfo.overlays.StrokeStyle
import com.tcatuw.goinfo.overlays.Style
import com.tcatuw.goinfo.quests.surface.AddCyclewayPartSurface
import com.tcatuw.goinfo.quests.surface.AddFootwayPartSurface
import com.tcatuw.goinfo.quests.surface.AddPathSurface
import com.tcatuw.goinfo.quests.surface.AddRoadSurface

class SurfaceOverlay : Overlay {

    override val title = R.string.overlay_surface
    override val icon = R.drawable.ic_quest_street_surface
    override val changesetComment = "Specify surfaces"
    override val wikiLink: String = "Key:surface"
    override val achievements = listOf(CAR, PEDESTRIAN, WHEELCHAIR, BICYCLIST, OUTDOORS)
    override val hidesQuestTypes = setOf(
        AddRoadSurface::class.simpleName!!,
        AddPathSurface::class.simpleName!!,
        AddFootwayPartSurface::class.simpleName!!,
        AddCyclewayPartSurface::class.simpleName!!,
    )

    override fun getStyledElements(mapData: MapDataWithGeometry): Sequence<Pair<Element, Style>> =
        mapData.filter("""
            ways, relations with
                highway ~ ${(ALL_PATHS + ALL_ROADS).joinToString("|")}
        """).map { it to getStyle(it) }

    override fun createForm(element: Element?) = SurfaceOverlayForm()
}

private fun getStyle(element: Element): Style {
    val isArea = element.tags["area"] == "yes"
    val isSegregated = element.tags["segregated"] == "yes"
    val isPath = element.tags["highway"] in ALL_PATHS

    val color = if (isPath && isSegregated) {
        val footwayColor = createSurfaceAndNote(element.tags, "footway").getColor(element)
        val cyclewayColor = createSurfaceAndNote(element.tags, "cycleway").getColor(element)
        // take worst case for showing
        listOf(footwayColor, cyclewayColor).minBy { color ->
            when (color) {
                Color.DATA_REQUESTED -> 0
                Color.INVISIBLE -> 1
                Color.BLACK -> 2
                else -> 3
            }
        }
    } else {
        createSurfaceAndNote(element.tags).getColor(element)
    }
    return if (isArea) PolygonStyle(color) else PolylineStyle(StrokeStyle(color))
}

private fun isMotorway(tags: Map<String, String>): Boolean =
    tags["highway"] == "motorway" || tags["highway"] == "motorway_link"
