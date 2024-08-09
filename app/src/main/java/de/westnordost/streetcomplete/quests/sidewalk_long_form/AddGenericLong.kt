package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import de.westnordost.streetcomplete.osm.MAXSPEED_TYPE_KEYS
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.osm.sidewalk.Sidewalk.INVALID
import de.westnordost.streetcomplete.osm.sidewalk.any
import de.westnordost.streetcomplete.osm.sidewalk.parseSidewalkSides
import de.westnordost.streetcomplete.osm.surface.UNPAVED_SURFACES
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest

class AddGenericLong(val item: AddLongFormResponseItem): OsmElementQuestType<Quest> {
    override val changesetComment = "Specify whether roads have sidewalks"
    override val wikiLink = "Key:sidewalk"
    override val icon = when (item.elementType) {
        "Sidewalks" -> R.drawable.ic_quest_sidewalk
        "Crossings" -> R.drawable.ic_quest_pedestrian_crossing
        else -> R.drawable.ic_quest_kerb_type
    }
    override val achievements = listOf(PEDESTRIAN)
    override val defaultDisabledMessage = R.string.default_disabled_msg_overlay

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("""
            ${getNodeOrWay(item.elementType!!)} with (
                ${item.questQuery}
              )
        """)

    override fun applyAnswerTo(
        answer: Quest,
        tags: Tags,
        geometry: ElementGeometry,
        timestampEdited: Long,
    ) {
        TODO("Not yet implemented")
    }

    override val hint = R.string.quest_street_side_puzzle_tutorial

    override fun getTitle(tags: Map<String, String>) = R.string.quest_sidewalk_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean =
        createRoadsFilter(item.questQuery!!, item.elementType!!).matches(element)

    override fun createForm() = AddGenericLongForm(item.quests)

    // override fun applyAnswerTo(answer: LeftAndRightSidewalk, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
    //     answer.applyTo(tags)
    // }
}

private fun getNodeOrWay(variable: String): String {
    return when(variable) {
        "Kerb" -> "nodes"
        else -> "ways"
    }
}

private fun createRoadsFilter(variable: String, elementType: String) = """
    ${getNodeOrWay(elementType)} with
      (
        (
          $variable
        )
      )
""".toElementFilterExpression()

// streets that do not have sidewalk tagging yet
/* the filter additionally filters out ways that are unlikely to have sidewalks:
 *
 * + unpaved roads
 * + roads that are probably not developed enough to have sidewalk (i.e. country roads)
 * + roads with a very low speed limit
 * + Also, anything explicitly tagged as no pedestrians or explicitly tagged that the sidewalk
 *   is mapped as a separate way OR that is tagged with that the cycleway is separate. If the
 *   cycleway is separate, the sidewalk is too for sure
 */
private val untaggedRoadsFilter by lazy { """
    ways with
      highway ~ motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential
      and !sidewalk and !sidewalk:both and !sidewalk:left and !sidewalk:right
      and (!maxspeed or maxspeed > 9 or maxspeed ~ [A-Z].*)
      and surface !~ ${UNPAVED_SURFACES.joinToString("|")}
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
    val sides = parseSidewalkSides(tags) ?: return false
    if (sides.any { it == INVALID || it == null }) return true
    return false
}
