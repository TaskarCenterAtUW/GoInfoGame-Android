package com.tcatuw.goinfo.quests.crossing_kerb_height

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BLIND
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.WHEELCHAIR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.kerb_height.AddKerbHeightForm
import com.tcatuw.goinfo.quests.kerb_height.KerbHeight

class AddCrossingKerbHeight : OsmElementQuestType<KerbHeight> {

    private val crossingFilter by lazy { """
        nodes with
          highway = crossing
          and foot != no
          and crossing
          and !kerb:left and !kerb:right
          and (
            !kerb
            or kerb ~ yes|unknown
            or kerb !~ no|rolled and kerb older today -8 years
          )
    """.toElementFilterExpression() }

    /* The quest should not be asked when the kerb situation can theoretically be tagged with
       greater detail, i.e. where the sidewalks are mapped as separate ways and hence there is a
       footway that crosses the road at the highway=crossing node: In that case, it would be
       possible to put the kerbs at their actual physical locations. */
    private val excludedWaysFilter by lazy { """
        ways with
          highway and access ~ private|no
          or highway ~ footway|path|cycleway
    """.toElementFilterExpression() }

    override val changesetComment = "Determine the heights of kerbs at crossings"
    override val wikiLink = "Key:kerb"
    override val icon = R.drawable.ic_quest_wheelchair_crossing
    override val achievements = listOf(BLIND, WHEELCHAIR, BICYCLIST)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_crossing_kerb_height_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        return mapData.nodes
            .filter { crossingFilter.matches(it) && it.id !in excludedWayNodeIds }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!crossingFilter.matches(element)) false else null

    override fun createForm() = AddKerbHeightForm()

    override fun applyAnswerTo(answer: KerbHeight, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("kerb", answer.osmValue)
    }
}
