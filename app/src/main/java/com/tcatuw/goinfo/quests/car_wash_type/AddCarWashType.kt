package com.tcatuw.goinfo.quests.car_wash_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.AUTOMATED
import com.tcatuw.goinfo.quests.car_wash_type.CarWashType.SELF_SERVICE
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddCarWashType : OsmFilterQuestType<List<CarWashType>>() {

    override val elementFilter = "nodes, ways with amenity = car_wash and !automated and !self_service"
    override val changesetComment = "Specify car wash types"
    override val wikiLink = "Tag:amenity=car_wash"
    override val icon = R.drawable.ic_quest_car_wash
    override val achievements = listOf(CAR)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_carWashType_title

    override fun createForm() = AddCarWashTypeForm()

    override fun applyAnswerTo(answer: List<CarWashType>, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        val isAutomated = answer.contains(AUTOMATED)
        tags["automated"] = isAutomated.toYesNo()

        val hasSelfService = answer.contains(SELF_SERVICE)
        val selfService = when {
            hasSelfService && answer.size == 1 -> "only"
            hasSelfService -> "yes"
            else -> "no"
        }
        tags["self_service"] = selfService
    }
}
