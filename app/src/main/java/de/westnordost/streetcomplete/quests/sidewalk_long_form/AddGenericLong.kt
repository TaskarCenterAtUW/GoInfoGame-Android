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
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AddGenericLong(val item: AddLongFormResponseItem) :
    OsmElementQuestType<List<LongFormQuest?>> {
    override val changesetComment = "Changes to ${item.elementType}"
    override val wikiLink = "Key:${item.elementType?.lowercase()}"
    override val icon = when (item.elementType) {
        "Sidewalks" -> R.drawable.ic_quest_sidewalk
        "Crossings" -> R.drawable.ic_quest_pedestrian_crossing
        else -> R.drawable.ic_quest_kerb_type
    }
    override val achievements = listOf(PEDESTRIAN)

    override val name: String
        get() = "AddGenericLong${item.elementType}"

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter(
            """
                          ${item.questQuery}

        """
        )

    override fun applyAnswerTo(
        answer: List<LongFormQuest?>,
        tags: Tags,
        geometry: ElementGeometry,
        timestampEdited: Long,
    ) {
        for (quest in answer) {
            if (quest != null) {
                tags[quest.questTag!!] = quest.userInput.toString()
            }
        }
        tags["ext:gig_complete"] = "yes"
        //time stamp to date
        val date = java.time.LocalDate.now(ZoneId.of("UTC"))
        val currentDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        tags["ext:gig_last_updated"] = currentDate
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
    return when (variable) {
        "Kerb" -> "nodes"
        else -> "ways"
    }
}

//          and ext:gig_complete !~ yes
//          and ext:gig_last_updated older today -0 days
//     and (!ext:gig_last_updated or ext:gig_last_updated older today -1 days)
private fun createRoadsFilter(variable: String, elementType: String) = """
     $variable and ext:gig_complete !~ yes
""".toElementFilterExpression()
