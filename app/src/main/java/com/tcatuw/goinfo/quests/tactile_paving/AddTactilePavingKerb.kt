package com.tcatuw.goinfo.quests.tactile_paving

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BLIND
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.kerb.couldBeAKerb
import com.tcatuw.goinfo.osm.kerb.findAllKerbNodes
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddTactilePavingKerb : OsmElementQuestType<Boolean> {

    private val eligibleKerbsFilter by lazy { """
        nodes with
          !tactile_paving
          or tactile_paving = unknown
          or tactile_paving = no and tactile_paving older today -4 years
          or tactile_paving = yes and tactile_paving older today -8 years
    """.toElementFilterExpression() }

    override val changesetComment = "Specify whether kerbs have tactile paving"
    override val wikiLink = "Key:tactile_paving"
    override val icon = R.drawable.ic_quest_kerb_tactile_paving
    override val enabledInCountries = COUNTRIES_WHERE_TACTILE_PAVING_IS_COMMON
    override val achievements = listOf(BLIND)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_tactile_paving_kerb_title

    override fun createForm() = TactilePavingForm()

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.findAllKerbNodes().filter { eligibleKerbsFilter.matches(it) }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!eligibleKerbsFilter.matches(element) || element !is Node || !element.couldBeAKerb()) false
        else null

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("tactile_paving", answer.toYesNo())
        if (tags["kerb"] != "no") {
            tags["barrier"] = "kerb"
        }
    }
}
