package com.tcatuw.goinfo.quests.roof_shape

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.meta.CountryInfo
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BUILDING
import com.tcatuw.goinfo.osm.BUILDINGS_WITH_LEVELS
import com.tcatuw.goinfo.osm.Tags

class AddRoofShape(
    private val getCountryInfoByLocation: (location: LatLon) -> CountryInfo,
) : OsmElementQuestType<RoofShape> {

    private val filter by lazy { """
        ways, relations with
          ((building:levels or roof:levels) or (building ~ ${BUILDINGS_WITH_LEVELS.joinToString("|")}))
          and !roof:shape and !3dr:type and !3dr:roof
          and building
          and building !~ no|construction
          and location != underground
          and ruins != yes
    """.toElementFilterExpression() }

    override val changesetComment = "Specify roof shapes"
    override val wikiLink = "Key:roof:shape"
    override val icon = R.drawable.ic_quest_roof_shape
    override val achievements = listOf(BUILDING)
    override val defaultDisabledMessage = R.string.default_disabled_msg_roofShape

    override fun getTitle(tags: Map<String, String>) = R.string.quest_roofShape_title

    override fun createForm() = AddRoofShapeForm()

    override fun getApplicableElements(mapData: MapDataWithGeometry) =
        mapData.filter { element ->
            filter.matches(element) && (
                (element.tags["roof:levels"]?.toFloatOrNull() ?: 0f) > 0f
                    || roofsAreUsuallyFlatAt(element, mapData) == false
                )
        }

    override fun isApplicableTo(element: Element): Boolean? {
        if (!filter.matches(element)) return false
        /* if it has 0 roof levels, or the roof levels aren't specified,
           the quest should only be shown in certain countries. But whether
           the element is in a certain country cannot be ascertained without the element's geometry */
        if ((element.tags["roof:levels"]?.toFloatOrNull() ?: 0f) == 0f) return null
        return true
    }

    private fun roofsAreUsuallyFlatAt(element: Element, mapData: MapDataWithGeometry): Boolean? {
        val center = mapData.getGeometry(element.type, element.id)?.center ?: return null
        return getCountryInfoByLocation(center).roofsAreUsuallyFlat
    }

    override fun applyAnswerTo(answer: RoofShape, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["roof:shape"] = answer.osmValue
    }
}
