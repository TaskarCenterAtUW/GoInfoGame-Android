package com.tcatuw.goinfo.quests.ferry

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.RARE
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddFerryAccessPedestrian : OsmFilterQuestType<Boolean>() {

    override val elementFilter = "ways, relations with route = ferry and !foot"
    override val changesetComment = "Specify ferry access for pedestrians"
    override val wikiLink = "Tag:route=ferry"
    override val icon = R.drawable.ic_quest_ferry_pedestrian
    override val hasMarkersAtEnds = true
    override val achievements = listOf(RARE, PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_ferry_pedestrian_title

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["foot"] = answer.toYesNo()
    }
}
