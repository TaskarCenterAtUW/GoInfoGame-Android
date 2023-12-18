package com.tcatuw.goinfo.quests.postbox_ref

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.quest.NoCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.POSTMAN
import com.tcatuw.goinfo.osm.Tags

class AddPostboxRef : OsmFilterQuestType<PostboxRefAnswer>() {

    override val elementFilter = """
        nodes with
        amenity = post_box
        and !ref and noref != yes and ref:signed != no and !~"ref:.*"
    """
    override val changesetComment = "Specify postbox refs"
    override val wikiLink = "Tag:amenity=post_box"
    override val icon = R.drawable.ic_quest_mail_ref
    override val isDeleteElementEnabled = true
    override val achievements = listOf(POSTMAN)
    // source: https://commons.wikimedia.org/wiki/Category:Post_boxes_by_country
    override val enabledInCountries = NoCountriesExcept(
        "FR", "GB", "GG", "IM", "JE", "MT", "IE", "SG", "CZ", "SK", "CH", "US"
    )

    override fun getTitle(tags: Map<String, String>) = R.string.quest_genericRef_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("nodes with amenity = post_box")

    override fun createForm() = AddPostboxRefForm()

    override fun applyAnswerTo(answer: PostboxRefAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            is NoVisiblePostboxRef -> tags["ref:signed"] = "no"
            is PostboxRef ->          tags["ref"] = answer.ref
        }
    }
}
