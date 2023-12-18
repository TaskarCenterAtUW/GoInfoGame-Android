package com.tcatuw.goinfo.quests.police_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.NoCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CITIZEN
import com.tcatuw.goinfo.osm.Tags

class AddPoliceType : OsmFilterQuestType<PoliceType>() {

    override val elementFilter = "nodes, ways with amenity = police and !operator"
    override val changesetComment = "Specify Italian police types"
    override val wikiLink = "Tag:amenity=police"
    override val icon = R.drawable.ic_quest_police
    override val enabledInCountries = NoCountriesExcept("IT")
    override val achievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_policeType_title

    override fun createForm() = AddPoliceTypeForm()

    override fun applyAnswerTo(answer: PoliceType, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["operator"] = answer.operatorName
        tags["operator:wikidata"] = answer.wikidata
    }
}
