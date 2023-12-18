package com.tcatuw.goinfo.quests.drinking_water_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CITIZEN
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags

class AddDrinkingWaterType : OsmFilterQuestType<DrinkingWaterType>() {

    override val elementFilter = """
        nodes with
        (
            (amenity = drinking_water and !disused:amenity)
            or
            (disused:amenity = drinking_water and !amenity and older today -1 years)
        )
        and (!seasonal or seasonal = no)
        and !man_made and !natural and !fountain and !pump
    """

    override val changesetComment = "Specify drinking water types"
    override val wikiLink = "Tag:amenity=drinking_water"
    override val icon = R.drawable.ic_quest_drinking_water // another icon?
    override val isDeleteElementEnabled = true
    override val achievements = listOf(CITIZEN, OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_drinking_water_type_title2

    override fun createForm() = AddDrinkingWaterTypeForm()

    override fun applyAnswerTo(answer: DrinkingWaterType, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags)
    }
}
