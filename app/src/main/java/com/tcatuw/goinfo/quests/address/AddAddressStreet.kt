package com.tcatuw.goinfo.quests.address

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.elementfilter.toElementFilterExpression
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Element
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.MapDataWithGeometry
import com.tcatuw.goinfo.data.osm.mapdata.Relation
import com.tcatuw.goinfo.data.osm.mapdata.filter
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.quest.AllCountriesExcept
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.POSTMAN
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.address.StreetOrPlaceName
import com.tcatuw.goinfo.osm.address.applyTo

class AddAddressStreet : OsmElementQuestType<StreetOrPlaceName> {

    private val filter by lazy { """
        nodes, ways, relations with
          (addr:housenumber or addr:housename) and !addr:street and !addr:place and !addr:block_number and !addr:substreet and !addr:parentstreet
          or addr:streetnumber and !addr:street and !addr:substreet and !addr:parentstreet
    """.toElementFilterExpression() }

    // #2112 - exclude indirect addr:street
    private val excludedWaysFilter by lazy { """
        ways with
          addr:street and addr:interpolation
    """.toElementFilterExpression() }

    override val changesetComment = "Specify street/place names to addresses"
    override val icon = R.drawable.ic_quest_housenumber_street
    override val wikiLink = "Key:addr"
    // In Japan, housenumbers usually have block numbers, not streets
    override val enabledInCountries = AllCountriesExcept("JP")
    override val achievements = listOf(POSTMAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_address_street_title2

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val excludedWayNodeIds = mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(HashSet()) { it.nodeIds }

        val associatedStreetRelations = mapData.relations.filter {
            val type = it.tags["type"]
            type == "associatedStreet" || type == "street"
        }

        val addressesWithoutStreet = mapData.filter { address ->
            filter.matches(address)
            && associatedStreetRelations.none { it.contains(address.type, address.id) }
            && address.id !in excludedWayNodeIds
        }

        return addressesWithoutStreet
    }

    /* cannot be determined because of the associated street relations */
    override fun isApplicableTo(element: Element): Boolean? =
        if (!filter.matches(element)) false else null

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("""
            nodes, ways, relations with
            (addr:housenumber or addr:housename or addr:conscriptionnumber or addr:streetnumber)
            and !name and !brand and !operator and !ref
        """.toElementFilterExpression())

    override fun createForm() = AddAddressStreetForm()

    override fun applyAnswerTo(answer: StreetOrPlaceName, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags)
    }
}

private fun Relation.contains(elementType: ElementType, elementId: Long): Boolean {
    return members.any { it.type == elementType && it.ref == elementId }
}
