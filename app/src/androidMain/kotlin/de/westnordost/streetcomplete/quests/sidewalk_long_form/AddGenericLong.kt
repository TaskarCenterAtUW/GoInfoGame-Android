package de.westnordost.streetcomplete.quests.sidewalk_long_form

import android.content.res.Resources
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.quest.AndroidQuest
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import de.westnordost.streetcomplete.util.firebase.FirebaseAnalyticsHelper
import org.koin.core.component.KoinComponent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AddGenericLong(val item: Elements) :
    OsmElementQuestType<List<LongFormQuest?>>, KoinComponent, AndroidQuest {

    val resources: Resources = getKoin().get()

    override val changesetComment = "Changes to ${item.elementType}"
    override val wikiLink = "Key:${item.elementType?.lowercase()}"
    override val icon = when (item.elementTypeIcon) {
        null -> when(item.elementType?.lowercase()){
            "kerb" -> R.drawable.ic_quest_kerb_type
            "crossings" -> R.drawable.ic_quest_pedestrian_crossing
            "sidewalks" -> R.drawable.ic_quest_sidewalk
            else -> R.drawable.ic_quest_notes
        }
        else -> {
            val iconResId = resources.getIdentifier(
                "ic_quest_${item.elementTypeIcon}",
                "drawable",
                resources.getResourcePackageName(R.drawable.ic_quest_notes)
            )
            if (iconResId != 0) iconResId
            else  R.drawable.ic_quest_notes // Fallback to default icon if not found
        }
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
                when (quest.userInput){
                    is UserInput.Single -> {
                        tags[quest.questTag] = (quest.userInput as UserInput.Single).answer!!
                    }
                    is UserInput.Multiple -> {
                        val multipleAnswers = (quest.userInput as UserInput.Multiple).answers
                        if(multipleAnswers.isNotEmpty()){
                            tags[quest.questTag] = multipleAnswers.joinToString(";")
                        }
                    }
                    null -> {}
                }
            }
        }
        tags["ext:gig_complete"] = "yes"
        //time stamp to date
        val date = java.time.LocalDate.now(ZoneId.of("UTC"))
        val currentDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        tags["ext:gig_last_updated"] = currentDate

        item.elementType?.let { FirebaseAnalyticsHelper.logQuestAnswered(it) }
    }

    override fun getTitle(tags: Map<String, String>) = when (item.elementType?.lowercase()) {
        "sidewalks" -> R.string.quest_sidewalk_title
        "crossings" -> R.string.quest_crossing_title2
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
