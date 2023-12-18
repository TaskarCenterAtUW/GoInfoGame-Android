package com.tcatuw.goinfo.quests.fuel_service

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.NoCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddFuelSelfService : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
        nodes, ways with
          amenity = fuel
          and !self_service
          and !automated
    """
    override val changesetComment = "Survey whether fuel stations provide self-service"
    override val wikiLink = "Key:self_service"
    override val icon = R.drawable.ic_quest_fuel_self_service
    override val achievements = listOf(CAR)
    override val enabledInCountries = NoCountriesExcept("IT", "UK")

    override fun getTitle(tags: Map<String, String>) = R.string.quest_fuelSelfService_title

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["self_service"] = answer.toYesNo()
    }
}
