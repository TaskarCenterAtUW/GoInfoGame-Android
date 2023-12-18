package com.tcatuw.goinfo.quests.traffic_signals_button

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.isCrossingWithTrafficSignals
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddTrafficSignalsButton : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
        nodes with
          crossing = traffic_signals
          and highway ~ crossing|traffic_signals
          and foot != no
          and !button_operated
    """
    override val changesetComment = "Specify whether traffic signals have a button for pedestrians"
    override val wikiLink = "Tag:highway=traffic_signals"
    override val icon = R.drawable.ic_quest_traffic_lights
    override val achievements = listOf(PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_traffic_signals_button_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter { it.isCrossingWithTrafficSignals() }.asSequence()

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["button_operated"] = answer.toYesNo()
    }
}
