package com.tcatuw.goinfo.overlays.way_lit

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.ALL_PATHS
import com.tcatuw.goinfo.osm.ALL_ROADS
import com.tcatuw.goinfo.osm.isPrivateOnFoot
import com.tcatuw.goinfo.osm.lit.LitStatus
import com.tcatuw.goinfo.osm.lit.createLitStatus
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PolygonStyle
import com.tcatuw.goinfo.overlays.PolylineStyle
import com.tcatuw.goinfo.overlays.StrokeStyle
import com.tcatuw.goinfo.overlays.Style
import com.tcatuw.goinfo.quests.way_lit.AddWayLit

class WayLitOverlay : Overlay {

    override val title = R.string.overlay_lit
    override val icon = R.drawable.ic_quest_lantern
    override val changesetComment = "Specify whether ways are lit"
    override val wikiLink: String = "Key:lit"
    override val achievements = listOf(PEDESTRIAN)
    override val hidesQuestTypes = setOf(AddWayLit::class.simpleName!!)

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        mapData
            .filter("ways, relations with highway ~ ${(ALL_ROADS + ALL_PATHS).joinToString("|")}")
            .map { it to getStyle(it) }

    override fun createForm(element: Element?) = WayLitOverlayForm()
}

private fun getStyle(element: Element): Style {
    val lit = createLitStatus(element.tags)
    // not set but indoor or private -> do not highlight as missing
    val isNotSetButThatsOkay = lit == null && (isIndoor(element.tags) || isPrivateOnFoot(element))
    val color = if (isNotSetButThatsOkay) Color.INVISIBLE else lit.color
    return if (element.tags["area"] == "yes") {
        PolygonStyle(color, null)
    } else {
        PolylineStyle(StrokeStyle(color))
    }
}

private val LitStatus?.color get() = when (this) {
    LitStatus.YES,
    LitStatus.UNSUPPORTED ->   Color.LIME
    LitStatus.NIGHT_AND_DAY -> Color.AQUAMARINE
    LitStatus.AUTOMATIC ->     Color.SKY
    LitStatus.NO ->            Color.BLACK
    null ->                    Color.DATA_REQUESTED
}

private fun isIndoor(tags: Map<String, String>): Boolean = tags["indoor"] == "yes"
