package com.tcatuw.goinfo.quests.bus_stop_shelter

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.COVERED
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.NO_SHELTER
import com.tcatuw.goinfo.quests.bus_stop_shelter.BusStopShelterAnswer.SHELTER

class AddBusStopShelter : OsmFilterQuestType<BusStopShelterAnswer>() {

    override val elementFilter = """
        nodes, ways, relations with
        (
          public_transport = platform
          or (highway = bus_stop and public_transport != stop_position)
        )
        and physically_present != no and naptan:BusStopType != HAR
        and !covered
        and location !~ underground|indoor
        and indoor != yes
        and tunnel != yes
        and (!level or level >= 0)
        and (!shelter or shelter older today -4 years)
    """
    /* Not asking again if it is covered because it means the stop itself is under a large
       building or roof building so this won't usually change */

    override val changesetComment = "Specify whether public transport stops have shelters"
    override val wikiLink = "Key:shelter"
    override val icon = R.drawable.ic_quest_bus_stop_shelter
    override val achievements = listOf(PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_busStopShelter_title2

    override fun createForm() = AddBusStopShelterForm()

    override fun applyAnswerTo(answer: BusStopShelterAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            SHELTER -> tags.updateWithCheckDate("shelter", "yes")
            NO_SHELTER -> tags.updateWithCheckDate("shelter", "no")
            COVERED -> {
                tags.remove("shelter")
                tags["covered"] = "yes"
            }
        }
    }
}
