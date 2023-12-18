package com.tcatuw.goinfo.quests.defibrillator

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.LIFESAVER
import com.tcatuw.goinfo.osm.Tags

class AddDefibrillatorLocation : OsmFilterQuestType<String>() {

    override val elementFilter = """
        nodes with
        emergency = defibrillator
        and !location and !defibrillator:location
        and access !~ private|no"
    """
    override val changesetComment = "Specify defibrillator location"
    override val wikiLink = "Tag:emergency=defibrillator"
    override val icon = R.drawable.ic_quest_defibrillator
    override val isDeleteElementEnabled = false
    override val achievements = listOf(LIFESAVER)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_defibrillator_location

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("nodes with emergency = defibrillator")

    override fun createForm() = AddDefibrillatorLocationForm()

    override fun applyAnswerTo(answer: String, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["defibrillator:location"] = answer
    }
}
