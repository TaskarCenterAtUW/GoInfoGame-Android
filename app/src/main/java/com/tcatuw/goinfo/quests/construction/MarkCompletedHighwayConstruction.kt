package com.tcatuw.goinfo.quests.construction

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.ALL_ROADS
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.toCheckDateString
import com.tcatuw.goinfo.osm.updateCheckDate

class MarkCompletedHighwayConstruction : OsmFilterQuestType<CompletedConstructionAnswer>() {

    override val elementFilter = """
        ways with highway = construction
         and (!opening_date or opening_date < today)
         and older today -2 weeks
    """
    override val changesetComment = "Determine whether road construction is now completed"
    override val wikiLink = "Tag:highway=construction"
    override val icon = R.drawable.ic_quest_road_construction
    override val hasMarkersAtEnds = true
    override val achievements = listOf(CAR)

    override fun getTitle(tags: Map<String, String>): Int {
        val isRoad = ALL_ROADS.contains(tags["construction"])
        val isCycleway = tags["construction"] == "cycleway"
        val isFootway = tags["construction"] == "footway"
        val isSteps = tags["construction"] == "steps"
        /* Alternative could be "Is this construction finished?" and just display the feature name
        *  (e.g. "cycleway under construction") of the element above, but there are no iD presets
        *  for "highway=construction + construction=*" so such road would just be named
        *  "Road closed". Hence, keeping this (for now). */
        return when {
            isRoad -> R.string.quest_construction_road_title
            isCycleway -> R.string.quest_construction_cycleway_title
            isFootway -> R.string.quest_construction_footway_title
            isSteps -> R.string.quest_construction_steps_title
            else -> R.string.quest_construction_generic_title
        }
    }

    override fun createForm() = MarkCompletedConstructionForm()

    override fun applyAnswerTo(answer: CompletedConstructionAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            is OpeningDateAnswer -> {
                tags["opening_date"] = answer.date.toCheckDateString()
            }
            is StateAnswer -> {
                if (answer.value) {
                    val value = tags["construction"] ?: "road"
                    tags["highway"] = value
                    removeTagsDescribingConstruction(tags)
                } else {
                    tags.updateCheckDate()
                }
            }
        }
    }
}
