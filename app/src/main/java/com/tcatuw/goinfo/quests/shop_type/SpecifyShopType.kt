package com.tcatuw.goinfo.quests.shop_type

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CITIZEN
import com.tcatuw.goinfo.osm.IS_SHOP_OR_DISUSED_SHOP_EXPRESSION
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.removeCheckDates

class SpecifyShopType : OsmFilterQuestType<ShopTypeAnswer>() {

    override val elementFilter = """
        nodes, ways with (
         shop = yes
         and !man_made
         and !historic
         and !military
         and !power
         and !tourism
         and !attraction
         and !amenity
         and !leisure
         and !aeroway
         and !railway
         and !craft
         and !healthcare
         and !office
        )
    """
    override val changesetComment = "Survey shop types"
    override val wikiLink = "Key:shop"
    override val icon = R.drawable.ic_quest_shop
    override val isReplaceShopEnabled = true
    override val achievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_shop_type_title2

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter(IS_SHOP_OR_DISUSED_SHOP_EXPRESSION)

    override fun createForm() = ShopTypeForm()

    override fun applyAnswerTo(answer: ShopTypeAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.removeCheckDates()
        when (answer) {
            is IsShopVacant -> {
                tags.remove("shop")
                tags["disused:shop"] = "yes"
            }
            is ShopType -> {
                tags.remove("disused:shop")
                if (!answer.tags.containsKey("shop")) {
                    tags.remove("shop")
                }
                for ((key, value) in answer.tags) {
                    tags[key] = value
                }
            }
        }
    }
}
