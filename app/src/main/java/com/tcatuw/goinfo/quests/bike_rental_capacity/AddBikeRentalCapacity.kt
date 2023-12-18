package com.tcatuw.goinfo.quests.bike_rental_capacity

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.bike_parking_capacity.AddBikeParkingCapacityForm

class AddBikeRentalCapacity : OsmFilterQuestType<Int>() {

    override val elementFilter = """
        nodes, ways with
         amenity = bicycle_rental
         and access !~ private|no
         and bicycle_rental = docking_station
         and (!capacity or capacity older today -6 years)
    """

    override val changesetComment = "Specify bicycle rental capacities"
    override val wikiLink = "Tag:amenity=bicycle_rental"
    override val icon = R.drawable.ic_quest_bicycle_rental_capacity
    override val isDeleteElementEnabled = true
    override val achievements = listOf(BICYCLIST)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_bicycle_rental_capacity_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("nodes, ways with amenity = bicycle_rental")

    override fun createForm() = AddBikeParkingCapacityForm.create(showClarificationText = false)

    override fun applyAnswerTo(answer: Int, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("capacity", answer.toString())
    }
}
