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
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormQuest
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.UserInput
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.isVisibleGiven
import de.westnordost.streetcomplete.util.firebase.FirebaseAnalyticsHelper
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.platform.HasName
import org.koin.core.component.KoinComponent

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

class AddGenericLong(val item: Elements, val recencyPeriodInDays : Int) :
    OsmElementQuestType<List<LongFormQuest?>>, KoinComponent, AndroidQuest, HasName {

    val resources: Resources = getKoin().get()
    override val changesetComment = "Changes to ${item.elementType}"
    override val wikiLink = "Key:${item.elementType?.lowercase()}"
    private val localIconResId: Int? = item.elementTypeIcon?.let { name ->
        resources.getIdentifier(
            "ic_quest_$name",
            "drawable",
            resources.getResourcePackageName(R.drawable.ic_quest_notes)
        ).takeIf { it != 0 }
    }

    override val icon = when (item.elementTypeIcon) {
        null -> when(item.elementType?.lowercase()){
            "kerb" -> R.drawable.ic_quest_kerb_type
            "crossings" -> R.drawable.ic_quest_pedestrian_crossing
            "sidewalks" -> R.drawable.ic_quest_sidewalk
            else -> R.drawable.ic_quest_notes
        }
        else -> localIconResId ?: R.drawable.ic_quest_notes // Fallback to default icon if not found
    }

    // non-null only when element_type_icon is set but didn't resolve to a local ic_quest_*
    // drawable - lets map-pin code check the workspace's custom-icons list (type="quest") for a
    // URL override, see FeaturePresetCatalog.questCustomIconFileOrNull
    val unresolvedIconName: String? =
        if (item.elementTypeIcon != null && localIconResId == null) item.elementTypeIcon else null
    override val achievements = listOf(PEDESTRIAN)

    override val name: String
        get() = item.elementType!!

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
            if (quest == null) continue
            val questTag = quest.questTag ?: continue
            // A null/empty userInput here means the user cleared a previously-answered question
            // (deselected a choice, emptied a text field) - remove the tag entirely rather than
            // leaving the stale prior value in place. Tags.remove() is a safe no-op if the tag
            // wasn't set to begin with.
            when (val input = quest.userInput) {
                is UserInput.Single -> {
                    val value = input.answer
                    if (value.isNullOrEmpty()) tags.remove(questTag) else tags[questTag] = value
                }
                is UserInput.Multiple -> {
                    if (input.answers.isNotEmpty()) tags[questTag] = input.answers.joinToString(";")
                    else tags.remove(questTag)
                }
                null -> tags.remove(questTag)
            }
        }
        item.elementType?.let { FirebaseAnalyticsHelper.logQuestAnswered(it) }
    }

    override fun getTitle(tags: Map<String, String>) = when (item.elementType?.lowercase()) {
        "sidewalks" -> R.string.quest_sidewalk_title
        "crossings" -> R.string.quest_crossing_title2
        else -> R.string.quest_kerb_title
    }

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean {
        if (!item.questQuery!!.toElementFilterExpression().matches(element)) return false
        if (item.quests.unansweredQuestions(element.tags).isNotEmpty()) return true
        // All applicable questions are answered - resurface for a recheck once the element's
        // own OSM last-edited timestamp is older than the workspace's recency period.
        val ageInMillis = nowAsEpochMilliseconds() - element.timestampEdited
        return ageInMillis >= recencyPeriodInDays * MILLIS_PER_DAY
    }

    override fun createForm() = AddGenericLongForm.newInstance(item.quests)

}

private fun getNodeOrWay(variable: String): String {
    return when (variable) {
        "Kerb" -> "nodes"
        else -> "ways"
    }
}

/** The subset of [this] question set that is still unanswered on [tags] - i.e. applicable per
 *  questAnswerDependency (see [LongFormQuest.isVisibleGiven]) but with no value yet for its
 *  questTag. Once this is empty, the quest is fully answered - the pin then only reappears once
 *  the element's own OSM timestamp is older than [AddGenericLong.recencyPeriodInDays]. */
private fun List<LongFormQuest?>.unansweredQuestions(tags: Map<String, String>): List<LongFormQuest> {
    val quests = filterNotNull()
    val byQuestId = quests.associateBy { it.questId }
    fun answersOf(id: Int): List<String>? {
        val quest = byQuestId[id] ?: return null
        val value = quest.questTag?.let { tags[it] } ?: return null
        return if (quest.questType == "MultipleChoice") value.split(";") else listOf(value)
    }
    return quests.filter { quest ->
        quest.isVisibleGiven({ byQuestId.containsKey(it) }, ::answersOf) &&
            (quest.questTag == null || tags[quest.questTag].isNullOrBlank())
    }
}
