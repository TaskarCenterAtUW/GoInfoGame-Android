package com.tcatuw.goinfo.quests.tracktype

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.surface.ANYTHING_UNPAVED
import com.tcatuw.goinfo.osm.updateWithCheckDate

class AddTracktype : OsmFilterQuestType<Tracktype>() {

    override val elementFilter = """
        ways with highway = track
        and (
          !tracktype
          or tracktype != grade1 and tracktype older today -6 years
          or surface ~ ${ANYTHING_UNPAVED.joinToString("|")} and tracktype older today -6 years
          or tracktype older today -8 years
        )
        and (access !~ private|no or (foot and foot !~ private|no))
    """
    /* ~paved tracks are less likely to change the surface type */
    override val changesetComment = "Specify tracktypes"
    override val wikiLink = "Key:tracktype"
    override val icon = R.drawable.ic_quest_tractor
    override val achievements = listOf(CAR, BICYCLIST)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_tracktype_title

    override fun createForm() = AddTracktypeForm()

    override fun applyAnswerTo(answer: Tracktype, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateWithCheckDate("tracktype", answer.osmValue)
    }
}
