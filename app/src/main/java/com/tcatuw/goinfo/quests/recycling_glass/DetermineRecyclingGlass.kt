package com.tcatuw.goinfo.quests.recycling_glass

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.AllCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CITIZEN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.quests.recycling_glass.RecyclingGlass.ANY
import com.tcatuw.goinfo.quests.recycling_glass.RecyclingGlass.BOTTLES

class DetermineRecyclingGlass : OsmFilterQuestType<RecyclingGlass>() {

    override val elementFilter = """
        nodes, ways with
          amenity = recycling
          and recycling_type = container
          and recycling:glass = yes
          and !recycling:glass_bottles
          and access !~ private|no
    """
    override val changesetComment = "Determine whether any glass or just glass bottles can be recycled here"
    override val wikiLink = "Key:recycling"
    override val icon = R.drawable.ic_quest_recycling_glass
    // see isUsuallyAnyGlassRecycleableInContainers.yml
    override val enabledInCountries = AllCountriesExcept("CZ")
    override val isDeleteElementEnabled = true
    override val achievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_recycling_glass_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("nodes with amenity = recycling")

    override fun createForm() = DetermineRecyclingGlassForm()

    override fun applyAnswerTo(answer: RecyclingGlass, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            ANY -> {
                // to mark that it has been checked
                tags["recycling:glass_bottles"] = "yes"
            }
            BOTTLES -> {
                tags["recycling:glass_bottles"] = "yes"
                tags["recycling:glass"] = "no"
            }
        }
    }
}
