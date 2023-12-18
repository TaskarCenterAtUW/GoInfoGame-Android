package com.tcatuw.goinfo.quests.orchard_produce

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags

class AddOrchardProduce : OsmFilterQuestType<List<OrchardProduce>>() {

    override val elementFilter = """
        ways, relations with landuse = orchard
        and !trees and !produce and !crop
        and orchard != meadow_orchard
    """
    override val changesetComment = "Specify orchard produces"
    override val wikiLink = "Tag:landuse=orchard"
    override val icon = R.drawable.ic_quest_apple
    override val achievements = listOf(OUTDOORS)
    override val defaultDisabledMessage = R.string.default_disabled_msg_difficult_and_time_consuming

    override fun getTitle(tags: Map<String, String>) = R.string.quest_orchard_produce_title

    override fun createForm() = AddOrchardProduceForm()

    override fun applyAnswerTo(answer: List<OrchardProduce>, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["produce"] = answer.joinToString(";") { it.osmValue }

        val landuse = answer.singleOrNull()?.osmLanduseValue
        if (landuse != null) {
            tags["landuse"] = landuse
        }
    }
}
