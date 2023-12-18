package com.tcatuw.goinfo.overlays.address

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.POSTMAN
import com.tcatuw.goinfo.overlays.Color
import com.tcatuw.goinfo.overlays.Overlay
import com.tcatuw.goinfo.overlays.PointStyle
import com.tcatuw.goinfo.overlays.PolygonStyle
import com.tcatuw.goinfo.quests.address.AddHousenumber
import com.tcatuw.goinfo.util.getShortHouseNumber

class AddressOverlay(
    private val getCountryCodeByLocation: (location: LatLon) -> String?
) : Overlay {

    override val title = R.string.overlay_addresses
    override val icon = R.drawable.ic_quest_housenumber
    override val changesetComment = "Survey housenumbers"
    override val wikiLink: String = "Key:addr"
    override val achievements = listOf(POSTMAN)
    override val hidesQuestTypes = setOf(AddHousenumber::class.simpleName!!)
    override val isCreateNodeEnabled = true

    override val sceneUpdates = listOf(
        "layers.housenumber-labels.enabled" to "false",
        "layers.buildings.draw.buildings-style.extrude" to "false",
        "layers.buildings.draw.buildings-outline-style.extrude" to "false"
    )

    private val noAddressesOnBuildings = setOf(
        "IT" // https://github.com/streetcomplete/StreetComplete/issues/4801
    )

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        mapData
            .filter("""
                nodes with
                  addr:housenumber or addr:housename or addr:conscriptionnumber or addr:streetnumber
                  or entrance
            """)
            .map { it to PointStyle(icon = null, label = getShortHouseNumber(it.tags) ?: "◽") } + // or ▫
        mapData
            .filter("ways, relations with building")
            .filter {
                val center = mapData.getGeometry(it.type, it.id)?.center ?: return@filter false
                val country = getCountryCodeByLocation(center)
                country !in noAddressesOnBuildings
            }
            .map { it to PolygonStyle(Color.INVISIBLE, label = getShortHouseNumber(it.tags)) }

    override fun createForm(element: Element?) = AddressOverlayForm()
}
