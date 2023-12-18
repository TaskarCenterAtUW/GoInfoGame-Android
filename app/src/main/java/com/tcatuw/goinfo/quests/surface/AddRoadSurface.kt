package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.BICYCLIST
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.CAR
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.surface.ANYTHING_UNPAVED
import com.tcatuw.goinfo.osm.surface.INVALID_SURFACES
import com.tcatuw.goinfo.osm.surface.INVALID_SURFACES_FOR_TRACKTYPES
import com.tcatuw.goinfo.osm.surface.SurfaceAndNote
import com.tcatuw.goinfo.osm.surface.applyTo

class AddRoadSurface : OsmFilterQuestType<SurfaceAndNote>() {

    override val elementFilter = """
        ways with (
          highway ~ ${listOf(
            "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
            "unclassified", "residential", "living_street", "pedestrian", "track",
            ).joinToString("|")
          }
          or highway = service and service !~ driveway|slipway
        )
        and (
          !surface
          or surface ~ ${ANYTHING_UNPAVED.joinToString("|")} and surface older today -6 years
          or surface older today -12 years
          or (
            surface ~ paved|unpaved|${INVALID_SURFACES.joinToString("|")}
            and !surface:note
            and !note:surface
          )
          ${INVALID_SURFACES_FOR_TRACKTYPES.map{tracktypeConflictClause(it)}.joinToString("\n")}
        )
        and (access !~ private|no or (foot and foot !~ private|no))
    """

    private fun tracktypeConflictClause(conflictEntry: Map.Entry<String, Set<String>>): String {
        return "          or tracktype = " + conflictEntry.key + " and surface ~ ${conflictEntry.value.joinToString("|")}"
    }

    override val changesetComment = "Specify road surfaces"
    override val wikiLink = "Key:surface"
    override val icon = R.drawable.ic_quest_street_surface
    override val achievements = listOf(CAR, BICYCLIST)

    override fun getTitle(tags: Map<String, String>) =
        if (tags["area"] == "yes") R.string.quest_streetSurface_square_title
        else                       R.string.quest_streetSurface_title

    override fun createForm() = AddRoadSurfaceForm()

    override fun applyAnswerTo(answer: SurfaceAndNote, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags)
    }
}
