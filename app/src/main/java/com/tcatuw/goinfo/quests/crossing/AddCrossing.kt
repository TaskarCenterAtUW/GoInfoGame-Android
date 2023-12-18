package com.tcatuw.goinfo.quests.crossing

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.findNodesAtCrossingsOf
import com.tcatuw.goinfo.osm.isCrossing
import com.tcatuw.goinfo.quests.crossing.CrossingAnswer.*

class AddCrossing : OsmElementQuestType<CrossingAnswer> {

    private val roadsFilter by lazy { """
        ways with
          highway ~ trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential
          and area != yes
          and (access !~ private|no or (foot and foot !~ private|no))
    """.toElementFilterExpression() }

    private val footwaysFilter by lazy { """
        ways with
          (highway ~ footway|steps or highway ~ path|cycleway and foot ~ designated|yes)
          and area != yes
          and access !~ private|no
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether there are crossings at intersections of paths and roads"
    override val wikiLink = "Tag:highway=crossing"
    override val icon = R.drawable.ic_quest_pedestrian
    override val achievements = listOf(PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_crossing_title2

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter { it.isCrossing() }.asSequence()

    override fun isApplicableTo(element: Element): Boolean? =
        if (element is Node && element.tags.isEmpty()) null else false

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val barrierWays = mapData.ways.asSequence()
            .filter { roadsFilter.matches(it) }

        val movingWays = mapData.ways.asSequence()
            .filter { footwaysFilter.matches(it) }

        var crossings = findNodesAtCrossingsOf(barrierWays, movingWays, mapData)

        /* require all roads at a shared node to either have no sidewalk tagging or all of them to
         * have sidewalk tagging: If the sidewalk tagging changes at that point, it may be an
         * indicator that this is the transition point between separate sidewalk mapping and
         * sidewalk mapping on road-way. E.g.:
         * https://www.openstreetmap.org/node/1839120490 */
        val anySidewalk = setOf("both", "left", "right")

        crossings = crossings.filter { crossing ->
            crossing.barrierWays.all { it.tags["sidewalk"] in anySidewalk } ||
            crossing.barrierWays.all { it.tags["sidewalk"] !in anySidewalk }
        }
        return crossings.map { it.node }.filter { it.tags.isEmpty() }
    }

    override fun createForm() = AddCrossingForm()

    override fun applyAnswerTo(answer: CrossingAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            YES -> tags["highway"] = "crossing"
            NO -> tags["crossing"] = "informal"
            PROHIBITED -> tags["crossing"] = "no"
        }
    }
}
