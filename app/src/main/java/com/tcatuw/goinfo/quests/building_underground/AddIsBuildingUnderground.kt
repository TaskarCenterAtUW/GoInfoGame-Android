package com.tcatuw.goinfo.quests.building_underground

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BUILDING
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.YesNoQuestForm

class AddIsBuildingUnderground : OsmFilterQuestType<Boolean>() {

    override val elementFilter = "ways, relations with building and layer ~ -[0-9]+ and !location"
    override val changesetComment = "Determine whether buildings are fully underground"
    override val wikiLink = "Key:location"
    override val icon = R.drawable.ic_quest_building_underground
    override val achievements = listOf(BUILDING)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_building_underground_title

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["location"] = if (answer) "underground" else "surface"
    }
}
