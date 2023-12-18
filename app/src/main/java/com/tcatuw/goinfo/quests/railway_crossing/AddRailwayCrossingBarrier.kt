package com.tcatuw.goinfo.quests.railway_crossing

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate

class AddRailwayCrossingBarrier : OsmElementQuestType<RailwayCrossingBarrier> {

    private val crossingFilter by lazy { """
        nodes with
          railway ~ level_crossing|crossing
          and (
            !crossing:barrier and !crossing:chicane
            or crossing:barrier older today -8 years
          )
    """.toElementFilterExpression() }

    private val excludedWaysFilter by lazy { """
        ways with
          highway and access ~ private|no
          or railway ~ tram|abandoned|disused
          or railway and embedded = yes
    """.toElementFilterExpression() }

    override val changesetComment = "Specify railway crossing barriers type"
    override val wikiLink = "Key:crossing:barrier"
    override val icon = R.drawable.ic_quest_railway
    override val achievements = listOf(CAR, PEDESTRIAN, BICYCLIST)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_railway_crossing_barrier_title2

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        return mapData.nodes
            .filter { crossingFilter.matches(it) && it.id !in excludedWayNodeIds }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!crossingFilter.matches(element)) false else null

    override fun createForm() = AddRailwayCrossingBarrierForm()

    override fun applyAnswerTo(answer: RailwayCrossingBarrier, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        if (answer.osmValue != null) {
            tags.remove("crossing:chicane")
            tags.updateWithCheckDate("crossing:barrier", answer.osmValue)
        }
        /* The mere existence of the crossing:chicane tag seems to imply that there could be a
        *  barrier additionally to the chicane.
        *  However, we still tag crossing:barrier=no here because the illustration as shown
        *  in the app corresponds to the below tagging - it shows just a chicane and no further
        *  barriers */
        if (answer == RailwayCrossingBarrier.CHICANE) {
            tags.updateWithCheckDate("crossing:barrier", "no")
            tags["crossing:chicane"] = "yes"
        }
    }
}
