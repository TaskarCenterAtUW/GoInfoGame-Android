package com.tcatuw.goinfo.quests.postbox_royal_cypher

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.NoCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.POSTMAN
import com.tcatuw.goinfo.osm.Tags

class AddPostboxRoyalCypher : OsmFilterQuestType<PostboxRoyalCypher>() {

    override val elementFilter = "nodes with amenity = post_box and !royal_cypher"
    override val changesetComment = "Specify postbox royal cyphers"
    override val wikiLink = "Key:royal_cypher"
    override val icon = R.drawable.ic_quest_crown
    override val isDeleteElementEnabled = true
    override val achievements = listOf(POSTMAN)
    override val enabledInCountries = NoCountriesExcept(
        // United Kingdom and some former nations of the British Empire, members of the Commonwealth of Nations and British overseas territories etc.
        "GB", "GI", "CY", "HK", "MT", "LK",
        // territories with agency postal services provided by the British Post Office
        "KW", "BH", "MA"
        // Not New Zealand: https://wiki.openstreetmap.org/w/index.php?title=Talk:StreetComplete/Quests&oldid=2599288#Quests_in_New_Zealand
    )

    override fun getTitle(tags: Map<String, String>) = R.string.quest_postboxRoyalCypher_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("nodes with amenity = post_box")

    override fun createForm() = AddPostboxRoyalCypherForm()

    override fun applyAnswerTo(answer: PostboxRoyalCypher, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["royal_cypher"] = answer.osmValue
    }
}
