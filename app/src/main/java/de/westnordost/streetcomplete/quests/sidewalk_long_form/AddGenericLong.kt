package de.westnordost.streetcomplete.quests.sidewalk_long_form

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Quest

class AddGenericLong(val item: AddLongFormResponseItem): OsmElementQuestType<Quest> {
    override val changesetComment = "Specify whether roads have sidewalks"
    override val wikiLink = "Key:sidewalk"
    override val icon = when (item.elementType) {
        "Sidewalks" -> R.drawable.ic_quest_sidewalk
        "Crossings" -> R.drawable.ic_quest_pedestrian_crossing
        else -> R.drawable.ic_quest_kerb_type
    }
    override val achievements = listOf(PEDESTRIAN)

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("""
            ${getNodeOrWay(item.elementType!!)} with (
                ${item.questQuery}
              )
        """)

    override fun applyAnswerTo(
        answer: Quest,
        tags: Tags,
        geometry: ElementGeometry,
        timestampEdited: Long,
    ) {
        TODO("Not yet implemented")
    }

    override fun getTitle(tags: Map<String, String>) = when (item.elementType) {
        "Sidewalks" -> R.string.quest_sidewalk_title
        "Crossings" -> R.string.quest_crossing_title2
        else -> R.string.quest_kerb_title
    }

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean =
        createRoadsFilter(item.questQuery!!, item.elementType!!).matches(element)

    override fun createForm() = AddGenericLongForm(item.quests)
}

private fun getNodeOrWay(variable: String): String {
    return when(variable) {
        "Kerb" -> "nodes"
        else -> "ways"
    }
}

private fun createRoadsFilter(variable: String, elementType: String) = """
    ${getNodeOrWay(elementType)} with
      (
        (
          $variable
        )
      )
""".toElementFilterExpression()
