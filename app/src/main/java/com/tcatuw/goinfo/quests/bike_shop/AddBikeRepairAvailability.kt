package com.tcatuw.goinfo.quests.bike_shop

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.osm.IS_SHOP_OR_DISUSED_SHOP_EXPRESSION
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.updateWithCheckDate
import com.tcatuw.goinfo.quests.YesNoQuestForm
import com.tcatuw.goinfo.util.ktx.toYesNo

class AddBikeRepairAvailability : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
        nodes, ways with shop = bicycle
        and (
            !service:bicycle:repair
            or service:bicycle:repair older today -6 years
        )
        and access !~ private|no
    """

    override val changesetComment = "Specify whether bicycle shops offer repairs"
    override val wikiLink = "Key:service:bicycle:repair"
    override val icon = R.drawable.ic_quest_bicycle_repair
    override val achievements = listOf(BICYCLIST)
    override val defaultDisabledMessage = R.string.default_disabled_msg_go_inside

    override fun getTitle(tags: Map<String, String>) = R.string.quest_bicycle_shop_repair_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter(IS_SHOP_OR_DISUSED_SHOP_EXPRESSION)

    override fun createForm() = YesNoQuestForm()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("service:bicycle:repair", answer.toYesNo())
    }
}
