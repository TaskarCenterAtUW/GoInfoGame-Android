package com.tcatuw.goinfo.overlays.cycleway

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.meta.CountryInfo
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.osm.ALL_ROADS
import com.tcatuw.goinfo.osm.MAXSPEED_TYPE_KEYS
import com.tcatuw.goinfo.osm.bicycle_boulevard.BicycleBoulevard
import com.tcatuw.goinfo.osm.bicycle_boulevard.createBicycleBoulevard
import com.tcatuw.goinfo.osm.cycleway.Cycleway
import com.tcatuw.goinfo.osm.cycleway.Cycleway.*
import com.tcatuw.goinfo.osm.cycleway.createCyclewaySides
import com.tcatuw.goinfo.osm.cycleway.isAmbiguous
import com.tcatuw.goinfo.osm.cycleway_separate.SeparateCycleway
import com.tcatuw.goinfo.osm.cycleway_separate.createSeparateCycleway
import com.tcatuw.goinfo.osm.isPrivateOnFoot
import com.tcatuw.goinfo.osm.surface.ANYTHING_UNPAVED
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PolylineStyle
import com.tcatuw.goinfo.overlays.StrokeStyle
import com.tcatuw.goinfo.quests.cycleway.AddCycleway

class CyclewayOverlay(
    private val getCountryInfoByLocation: (location: LatLon) -> CountryInfo,
) : Overlay {

    override val title = R.string.overlay_cycleway
    override val icon = R.drawable.ic_quest_bicycleway
    override val changesetComment = "Specify whether there are cycleways"
    override val wikiLink: String = "Key:cycleway"
    override val achievements = listOf(EditTypeAchievement.BICYCLIST)
    override val hidesQuestTypes = setOf(AddCycleway::class.simpleName!!)

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        // roads
        mapData.filter("""
            ways with
              highway ~ ${ALL_ROADS.joinToString("|")}
              and area != yes
        """).mapNotNull {
            val pos = mapData.getWayGeometry(it.id)?.center ?: return@mapNotNull null
            val countryInfo = getCountryInfoByLocation(pos)
            it to getStreetCyclewayStyle(it, countryInfo)
        } +
        // separately mapped ways
        mapData.filter("""
            ways with
              highway ~ cycleway|path|footway
              and horse != designated
              and area != yes
        """).map { it to getSeparateCyclewayStyle(it) }

    override fun createForm(element: Element?) =
        if (element == null) null
        else if (element.tags["highway"] in ALL_ROADS) StreetCyclewayOverlayForm()
        else SeparateCyclewayForm()
}

private fun getSeparateCyclewayStyle(element: Element) =
    PolylineStyle(StrokeStyle(createSeparateCycleway(element.tags).getColor()))

private fun SeparateCycleway?.getColor() = when (this) {
    SeparateCycleway.NOT_ALLOWED,
    SeparateCycleway.ALLOWED_ON_FOOTWAY,
    SeparateCycleway.NON_DESIGNATED,
    SeparateCycleway.PATH ->
        Color.BLACK

    SeparateCycleway.NON_SEGREGATED ->
        Color.CYAN

    SeparateCycleway.SEGREGATED,
    SeparateCycleway.EXCLUSIVE,
    SeparateCycleway.EXCLUSIVE_WITH_SIDEWALK ->
        Color.BLUE

    null ->
        Color.INVISIBLE
}

private fun getStreetCyclewayStyle(element: Element, countryInfo: CountryInfo): PolylineStyle {
    val cycleways = createCyclewaySides(element.tags, countryInfo.isLeftHandTraffic)
    val isBicycleBoulevard = createBicycleBoulevard(element.tags) == BicycleBoulevard.YES

    // not set but on road that usually has no cycleway or it is private -> do not highlight as missing
    val isNoCyclewayExpected =
        cycleways == null && (cyclewayTaggingNotExpected(element) || isPrivateOnFoot(element))

    return PolylineStyle(
        stroke = when {
            isBicycleBoulevard ->   StrokeStyle(Color.GOLD, dashed = true)
            isNoCyclewayExpected -> StrokeStyle(Color.INVISIBLE)
            else ->                 null
        },
        strokeLeft = if (isNoCyclewayExpected) null else cycleways?.left?.cycleway.getStyle(countryInfo),
        strokeRight = if (isNoCyclewayExpected) null else cycleways?.right?.cycleway.getStyle(countryInfo)
    )
}

private val cyclewayTaggingNotExpectedFilter by lazy { """
    ways with
      highway ~ track|living_street|pedestrian|service|motorway_link|motorway
      or motorroad = yes
      or expressway = yes
      or maxspeed <= 20
      or cyclestreet = yes
      or bicycle_road = yes
      or surface ~ ${ANYTHING_UNPAVED.joinToString("|")}
      or ~"${(MAXSPEED_TYPE_KEYS + "maxspeed").joinToString("|")}" ~ ".*:(zone)?:?([1-9]|[1-2][0-9]|30)"
""".toElementFilterExpression() }

private fun cyclewayTaggingNotExpected(element: Element) =
    cyclewayTaggingNotExpectedFilter.matches(element)

private fun Cycleway?.getStyle(countryInfo: CountryInfo) = when (this) {
    TRACK ->
        StrokeStyle(Color.BLUE)

    EXCLUSIVE_LANE, UNSPECIFIED_LANE ->
        if (isAmbiguous(countryInfo)) StrokeStyle(Color.DATA_REQUESTED)
        else                          StrokeStyle(Color.GOLD)

    ADVISORY_LANE, SUGGESTION_LANE, UNSPECIFIED_SHARED_LANE ->
        if (isAmbiguous(countryInfo)) StrokeStyle(Color.DATA_REQUESTED)
        else                          StrokeStyle(Color.ORANGE)

    PICTOGRAMS ->
        StrokeStyle(Color.ORANGE, dashed = true)

    UNKNOWN, INVALID, null, UNKNOWN_LANE, UNKNOWN_SHARED_LANE ->
        StrokeStyle(Color.DATA_REQUESTED)

    BUSWAY ->
        StrokeStyle(Color.LIME, dashed = true)

    SIDEWALK_EXPLICIT ->
        StrokeStyle(Color.CYAN, dashed = true)

    NONE ->
        StrokeStyle(Color.BLACK)

    SHOULDER, NONE_NO_ONEWAY ->
        StrokeStyle(Color.BLACK, dashed = true)

    SEPARATE ->
        StrokeStyle(Color.INVISIBLE)
}
