package com.tcatuw.goinfo.quests.sidewalk

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.MAXSPEED_TYPE_KEYS
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.sidewalk.LeftAndRightSidewalk
import com.tcatuw.goinfo.osm.sidewalk.Sidewalk.INVALID
import com.tcatuw.goinfo.osm.sidewalk.any
import com.tcatuw.goinfo.osm.sidewalk.applyTo
import com.tcatuw.goinfo.osm.sidewalk.createSidewalkSides
import com.tcatuw.goinfo.osm.surface.ANYTHING_UNPAVED

class AddSidewalk : OsmElementQuestType<LeftAndRightSidewalk> {
    override val changesetComment = "Specify whether roads have sidewalks"
    override val wikiLink = "Key:sidewalk"
    override val icon = R.drawable.ic_quest_sidewalk
    override val achievements = listOf(PEDESTRIAN)
    override val defaultDisabledMessage = R.string.default_disabled_msg_overlay

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("""
            ways with (
                highway ~ path|footway|steps
                or highway ~ cycleway|bridleway and foot ~ yes|designated
              )
              and foot !~ no|private
              and access !~ no|private
        """)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_sidewalk_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean =
        roadsFilter.matches(element)
        && (untaggedRoadsFilter.matches(element) || element.hasInvalidOrIncompleteSidewalkTags())

    override fun createForm() = AddSidewalkForm()

    override fun applyAnswerTo(answer: LeftAndRightSidewalk, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags)
    }
}

// streets that may have sidewalk tagging
private val roadsFilter by lazy { """
    ways with
      (
        (
          highway ~ trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|service
          and motorroad != yes
          and expressway != yes
          and foot != no
        )
        or
        (
          highway ~ motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|service
          and (foot ~ yes|designated or bicycle ~ yes|designated)
        )
      )
      and area != yes
      and access !~ private|no
""".toElementFilterExpression() }

// streets that do not have sidewalk tagging yet
/* the filter additionally filters out ways that are unlikely to have sidewalks:
 *
 * + unpaved roads
 * + roads that are probably not developed enough to have sidewalk (i.e. country roads)
 * + roads with a very low speed limit
 * + Also, anything explicitly tagged as no pedestrians or explicitly tagged that the sidewalk
 *   is mapped as a separate way OR that is tagged with that the cycleway is separate. If the
 *   cycleway is separate, the sidewalk is too for sure
* */
private val untaggedRoadsFilter by lazy { """
    ways with
      highway ~ motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential
      and !sidewalk and !sidewalk:both and !sidewalk:left and !sidewalk:right
      and (!maxspeed or maxspeed > 9 or maxspeed ~ [A-Z].*)
      and surface !~ ${ANYTHING_UNPAVED.joinToString("|")}
      and (
        lit = yes
        or highway = residential
        or ~"${(MAXSPEED_TYPE_KEYS + "maxspeed").joinToString("|")}" ~ ".*:(urban|.*zone.*|nsl_restricted)"
        or maxspeed <= 60
        or (foot ~ yes|designated and highway ~ motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link)
      )
      and ~foot|bicycle|bicycle:backward|bicycle:forward !~ use_sidepath
      and ~cycleway|cycleway:left|cycleway:right|cycleway:both !~ separate
""".toElementFilterExpression() }

private fun Element.hasInvalidOrIncompleteSidewalkTags(): Boolean {
    val sides = createSidewalkSides(tags) ?: return false
    if (sides.any { it == INVALID || it == null }) return true
    return false
}
