package com.tcatuw.goinfo.quests.leaf_detail

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.geometry.ElementPolygonsGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.util.math.measuredMultiPolygonArea

class AddForestLeafType : OsmElementQuestType<ForestLeafType> {
    private val areaFilter by lazy { """
        ways, relations with (landuse = forest or natural = wood) and !leaf_type
    """.toElementFilterExpression() }

    private val wayFilter by lazy { """
        ways with natural = tree_row and !leaf_type
    """.toElementFilterExpression() }

    override val changesetComment = "Specify leaf types"
    override val wikiLink = "Key:leaf_type"
    override val icon = R.drawable.ic_quest_leaf
    override val achievements = listOf(OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_leafType_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val forests = mapData
            .filter { areaFilter.matches(it) }
            .filter {
                val geometry = mapData.getGeometry(it.type, it.id) as? ElementPolygonsGeometry
                val area = geometry?.polygons?.measuredMultiPolygonArea() ?: 0.0
                area > 0.0 && area < 10000
            }
        val treeRows = mapData.filter { wayFilter.matches(it) }
        return forests + treeRows
    }

    override fun isApplicableTo(element: Element): Boolean? {
        if (wayFilter.matches(element)) return true // tree rows
        // for areas, we don't want to show things larger than x m², we need the geometry for that
        if (!areaFilter.matches(element)) return false
        return null
    }

    override fun createForm() = AddForestLeafTypeForm()

    override fun applyAnswerTo(answer: ForestLeafType, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["leaf_type"] = answer.osmValue
    }
}
