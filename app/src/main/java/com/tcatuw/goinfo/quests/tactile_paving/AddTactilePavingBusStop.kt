package com.tcatuw.goinfo.quests.tactile_paving

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BLIND
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddTactilePavingBusStop : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
        nodes, ways, relations with
        (
          public_transport = platform
          or (highway = bus_stop and public_transport != stop_position)
        )
        and physically_present != no and naptan:BusStopType != HAR
        and (
          !tactile_paving
          or tactile_paving = unknown
          or tactile_paving = no and tactile_paving older today -4 years
          or tactile_paving = yes and tactile_paving older today -8 years
        )
    """
    override val changesetComment = "Specify whether public transport stops have tactile paving"
    override val wikiLink = "Key:tactile_paving"
    override val icon = R.drawable.ic_quest_blind_bus
    override val enabledInCountries = COUNTRIES_WHERE_TACTILE_PAVING_IS_COMMON
    override val achievements = listOf(BLIND)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_busStopTactilePaving_title

    override fun createForm() = TactilePavingForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("tactile_paving", answer.toYesNo())
    }
}
