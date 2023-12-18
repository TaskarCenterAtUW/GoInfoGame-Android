package com.tcatuw.goinfo.quests.crossing_island

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BLIND
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.isCrossing
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddCrossingIsland : OsmElementQuestType<Boolean> {

    private val crossingFilter by lazy { """
        nodes with
          highway = crossing
          and foot != no
          and crossing
          and crossing != island
          and !crossing:island
    """.toElementFilterExpression() }

    private val excludedWaysFilter by lazy { """
        ways with
          highway and access ~ private|no
          or railway
          or highway = service
          or highway and oneway and oneway != no
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether pedestrian crossings have islands"
    override val wikiLink = "Key:crossing:island"
    override val icon = R.drawable.ic_quest_pedestrian_crossing_island
    override val achievements = listOf(PEDESTRIAN, BLIND)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_pedestrian_crossing_island

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter { it.isCrossing() }.asSequence()

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        return mapData.nodes
            .filter { crossingFilter.matches(it) && it.id !in excludedWayNodeIds }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!crossingFilter.matches(element)) false else null

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["crossing:island"] = answer.toYesNo()
    }
}
