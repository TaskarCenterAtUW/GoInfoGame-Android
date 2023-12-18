package com.tcatuw.goinfo.quests.step_count

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags

class AddStepCountStile : OsmElementQuestType<Int> {

    private val stileNodeFilter by lazy { """
        nodes with
          barrier = stile
          and stile ~ stepover|ladder
          and access !~ private|no
          and !step_count
    """.toElementFilterExpression() }

    private val excludedWaysFilter by lazy { """
        ways with
          access ~ private|no
          and foot !~ permissive|yes|designated
    """.toElementFilterExpression() }

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        return mapData.nodes
            .filter { stileNodeFilter.matches(it) && it.id !in excludedWayNodeIds }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!stileNodeFilter.matches(element)) false else null

    override val changesetComment = "Specify stiles step count"
    override val wikiLink = "Key:step_count"
    override val icon = R.drawable.ic_quest_steps_count_brown
    override val achievements = listOf(OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_step_count_title

    override fun createForm() = AddStepCountForm.create(R.string.quest_step_count_stile_hint)

    override fun applyAnswerTo(answer: Int, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["step_count"] = answer.toString()
    }
}
