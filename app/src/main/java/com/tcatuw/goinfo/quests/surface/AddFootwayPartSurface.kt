package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.WHEELCHAIR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.surface.SurfaceAndNote
import com.tcatuw.goinfo.osm.surface.applyTo
import com.tcatuw.goinfo.osm.surface.updateCommonSurfaceFromFootAndCyclewaySurface

class AddFootwayPartSurface : OsmFilterQuestType<SurfaceAndNote>() {

    override val elementFilter = """
        ways with (
          highway = footway
          or highway = path and foot != no
          or (highway ~ cycleway|bridleway and foot and foot != no)
        )
        and segregated = yes
        and !sidewalk
        and (
          !footway:surface
          or footway:surface older today -8 years
          or (
            footway:surface ~ paved|unpaved
            and !footway:surface:note
            and !note:footway:surface
          )
        )
        and (access !~ private|no or (foot and foot !~ private|no))
        and ~path|footway|cycleway|bridleway !~ link
    """
    override val changesetComment = "Add footway path surfaces"
    override val wikiLink = "Key:surface"
    override val icon = R.drawable.ic_quest_footway_surface
    override val achievements = listOf(PEDESTRIAN, WHEELCHAIR, OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_footwayPartSurface_title

    override fun createForm() = AddPathPartSurfaceForm()

    override fun applyAnswerTo(answer: SurfaceAndNote, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags, "footway")
        updateCommonSurfaceFromFootAndCyclewaySurface(tags)
    }
}
