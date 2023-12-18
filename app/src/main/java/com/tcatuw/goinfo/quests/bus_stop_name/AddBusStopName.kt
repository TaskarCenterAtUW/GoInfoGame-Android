package com.tcatuw.goinfo.quests.bus_stop_name

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.AllCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.applyTo

class AddBusStopName : OsmFilterQuestType<BusStopNameAnswer>() {

    override val elementFilter = """
        nodes, ways, relations with
        (
          public_transport = platform and bus = yes
          or (highway = bus_stop and public_transport != stop_position)
          or railway = halt
          or railway = station
          or railway = tram_stop
        )
        and !name and noname != yes and name:signed != no
    """

    override val enabledInCountries = AllCountriesExcept("US", "CA")
    override val changesetComment = "Determine public transport stop names"
    override val wikiLink = "Tag:public_transport=platform"
    override val icon = R.drawable.ic_quest_bus_stop_name
    override val achievements = listOf(PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_busStopName_title2

    override fun createForm() = AddBusStopNameForm()

    override fun applyAnswerTo(answer: BusStopNameAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            is NoBusStopName -> {
                tags["name:signed"] = "no"
            }
            is BusStopName -> {
                answer.localizedNames.applyTo(tags)
            }
        }
    }
}
