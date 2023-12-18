package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.osmquests.OsmFilterQuestType
import com.tcatuw.goinfo.data.user.achievements.EditTypeAchievement.OUTDOORS
import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.surface.SurfaceAndNote
import com.tcatuw.goinfo.osm.surface.applyTo

class AddPitchSurface : OsmFilterQuestType<SurfaceAndNote>() {
    private val sportValuesWherePitchSurfaceQuestionIsInteresting = listOf(
        // #2377
        "multi", "soccer", "tennis", "basketball", "equestrian", "athletics", "volleyball",
        "bmx", "american_football", "badminton", "pelota", "horse_racing", "skateboard",
        "disc_golf", "futsal", "cycling", "gymnastics", "bowls", "boules", "netball",
        "handball", "team_handball", "field_hockey", "padel", "horseshoes", "tetherball",
        "gaelic_games", "australian_football", "racquet", "rugby_league", "rugby_union", "rugby",
        "canadian_football", "softball", "sepak_takraw", "cricket", "pickleball", "lacrosse",
        "roller_skating", "baseball", "shuffleboard", "paddle_tennis", "korfball", "petanque",
        "croquet", "four_square", "shot-put",

        // #2468
        "running", "dog_racing", "toboggan",
    )

    override val elementFilter = """
        ways with leisure ~ pitch|track
         and sport ~ "(^|.*;)(${sportValuesWherePitchSurfaceQuestionIsInteresting.joinToString("|")})($|;.*)"
         and (access !~ private|no)
         and indoor != yes and (!building or building = no)
         and (
          !surface
          or surface older today -12 years
          or (
           surface ~ paved|unpaved
           and !surface:note
           and !note:surface
          )
        )
    """

    override val changesetComment = "Specify pitch surfaces"
    override val wikiLink = "Key:surface"
    override val icon = R.drawable.ic_quest_pitch_surface
    override val achievements = listOf(OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_surface_title

    override fun createForm() = AddPitchSurfaceForm()

    override fun applyAnswerTo(answer: SurfaceAndNote, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        answer.applyTo(tags)
    }
}
