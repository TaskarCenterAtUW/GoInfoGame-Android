package com.tcatuw.goinfo.quests.religion

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags

class AddReligionToWaysideShrine : OsmFilterQuestType<Religion>() {

    override val elementFilter = """
        nodes, ways, relations with
          historic = wayside_shrine
          and !religion
          and access !~ private|no
    """
    override val changesetComment = "Specify religion for wayside shrines"
    override val wikiLink = "Key:religion"
    override val icon = R.drawable.ic_quest_religion
    override val achievements = listOf(OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_religion_for_wayside_shrine_title

    override fun createForm() = AddReligionForm()

    override fun applyAnswerTo(answer: Religion, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["religion"] = answer.osmValue
    }
}
