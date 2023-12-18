package com.tcatuw.goinfo.overlays.shops

import de.westnordost.osmfeatures.Feature
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement
import com.tcatuw.goinfo.osm.IS_SHOP_OR_DISUSED_SHOP_EXPRESSION
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PointStyle
import com.tcatuw.goinfo.overlays.PolygonStyle
import com.tcatuw.goinfo.quests.place_name.AddPlaceName
import com.tcatuw.goinfo.quests.shop_type.CheckShopType
import com.tcatuw.goinfo.quests.shop_type.SpecifyShopType
import com.tcatuw.goinfo.util.getNameLabel

class ShopsOverlay(private val getFeature: (tags: Map<String, String>) -> Feature?) : Overlay {

    override val title = R.string.overlay_shops
    override val icon = R.drawable.ic_quest_shop
    override val changesetComment = "Survey shops etc."
    override val wikiLink: String = "Key:shop"
    override val achievements = listOf(EditTypeAchievement.CITIZEN)
    override val hidesQuestTypes = setOf(
        AddPlaceName::class.simpleName!!,
        SpecifyShopType::class.simpleName!!,
        CheckShopType::class.simpleName!!
    )
    override val isCreateNodeEnabled = true

    override val sceneUpdates = listOf(
        "layers.buildings.draw.buildings-style.extrude" to "false",
        "layers.buildings.draw.buildings-outline-style.extrude" to "false"
    )

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        mapData
            .filter(IS_SHOP_OR_DISUSED_SHOP_EXPRESSION)
            .map { element ->
                val feature = getFeature(element.tags)

                val icon = "ic_preset_" + (feature?.icon ?: "maki-shop" ).replace('-', '_')
                val label = getNameLabel(element.tags)

                val style = if (element is Node) {
                    PointStyle(icon, label)
                } else {
                    PolygonStyle(Color.INVISIBLE, icon, label)
                }
                element to style
            }

    override fun createForm(element: Element?) = ShopsOverlayForm()
}
