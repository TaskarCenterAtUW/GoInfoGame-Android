package com.tcatuw.goinfo.overlays.sidewalk

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.ALL_ROADS
import com.tcatuw.goinfo.osm.MAXSPEED_TYPE_KEYS
import com.tcatuw.goinfo.osm.cycleway_separate.SeparateCycleway
import com.tcatuw.goinfo.osm.cycleway_separate.createSeparateCycleway
import com.tcatuw.goinfo.osm.isPrivateOnFoot
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk
import com.tcatuw.goinfo.osm.sidewalk.any
import com.tcatuw.goinfo.osm.sidewalk.createSidewalkSides
import com.tcatuw.goinfo.osm.surface.ANYTHING_UNPAVED
import com.tcatuw.goinfo.overlays.AbstractOverlayForm
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PolylineStyle
import com.tcatuw.goinfo.overlays.StrokeStyle
import com.tcatuw.goinfo.quests.sidewalk.AddSidewalk

class SidewalkOverlay : Overlay {

    override val title = R.string.overlay_sidewalk
    override val icon = R.drawable.ic_quest_sidewalk
    override val changesetComment = "Specify whether roads have sidewalks"
    override val wikiLink: String = "Key:sidewalk"
    override val achievements = listOf(PEDESTRIAN)
    override val hidesQuestTypes = setOf(AddSidewalk::class.simpleName!!)

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        // roads
        mapData.filter("""
            ways with
              highway ~ motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|living_street|pedestrian|service
              and area != yes
        """).map { it to getSidewalkStyle(it) } +
        // footways etc, just to highlight e.g. separately mapped sidewalks. However, it is also
        // possible to add sidewalks to them. At least in NL, cycleways with sidewalks actually exist
        mapData.filter("""
            ways with (
              highway ~ footway|steps|path|bridleway|cycleway
            ) and area != yes
        """).map { it to getFootwayStyle(it) }

    override fun createForm(element: Element?): AbstractOverlayForm? {
        if (element == null) return null

        // allow editing of all roads and all exclusive cycleways
        return if (
            element.tags["highway"] in ALL_ROADS ||
            createSeparateCycleway(element.tags) in listOf(SeparateCycleway.EXCLUSIVE, SeparateCycleway.EXCLUSIVE_WITH_SIDEWALK)
        ) SidewalkOverlayForm() else null
    }
}

private fun getFootwayStyle(element: Element): PolylineStyle {
    val foot = element.tags["foot"] ?: when (element.tags["highway"]) {
        "footway", "steps" -> "designated"
        "path" -> "yes"
        else -> null
    }

    return when {
        createSidewalkSides(element.tags)?.any { it == Sidewalk.YES } == true ->
            getSidewalkStyle(element)
        foot in listOf("yes", "designated") ->
            PolylineStyle(StrokeStyle(Color.SKY))
        else ->
            PolylineStyle(StrokeStyle(Color.INVISIBLE))
    }
}

private fun getSidewalkStyle(element: Element): PolylineStyle {
    val sidewalkSides = createSidewalkSides(element.tags)
    // not set but on road that usually has no sidewalk or it is private -> do not highlight as missing
    if (sidewalkSides == null) {
        if (sidewalkTaggingNotExpected(element) || isPrivateOnFoot(element)) {
            return PolylineStyle(StrokeStyle(Color.INVISIBLE))
        }
    }

    return PolylineStyle(
        stroke = null,
        strokeLeft = sidewalkSides?.left.style,
        strokeRight = sidewalkSides?.right.style
    )
}

private val sidewalkTaggingNotExpectedFilter by lazy { """
    ways with
      highway ~ living_street|pedestrian|service|motorway_link
      or motorroad = yes
      or expressway = yes
      or maxspeed <= 10
      or maxspeed = walk
      or surface ~ ${ANYTHING_UNPAVED.joinToString("|")}
      or ~"${(MAXSPEED_TYPE_KEYS + "maxspeed").joinToString("|")}" ~ ".*:(zone)?:?([1-9]|10)"
""".toElementFilterExpression() }

private fun sidewalkTaggingNotExpected(element: Element) =
    sidewalkTaggingNotExpectedFilter.matches(element)

private val Sidewalk?.style get() = StrokeStyle(when (this) {
    Sidewalk.YES           -> Color.SKY
    Sidewalk.NO            -> Color.BLACK
    Sidewalk.SEPARATE      -> Color.INVISIBLE
    Sidewalk.INVALID, null -> Color.DATA_REQUESTED
})
