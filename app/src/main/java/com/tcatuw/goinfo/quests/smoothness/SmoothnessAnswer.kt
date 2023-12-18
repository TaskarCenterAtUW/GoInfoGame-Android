package com.tcatuw.goinfo.quests.smoothness

import com.tcatuw.goinfo.osm.Tags
import com.tcatuw.goinfo.osm.changeToSteps
import com.tcatuw.goinfo.osm.removeCheckDatesForKey
import com.tcatuw.goinfo.osm.updateWithCheckDate

sealed interface SmoothnessAnswer

data class SmoothnessValueAnswer(val value: Smoothness) : SmoothnessAnswer

object IsActuallyStepsAnswer : SmoothnessAnswer
object WrongSurfaceAnswer : SmoothnessAnswer

fun SmoothnessAnswer.applyTo(tags: Tags) {
    tags.remove("smoothness:date")
    // similar tag as smoothness, will be wrong/outdated when smoothness is set
    tags.remove("surface:grade")
    when (this) {
        is SmoothnessValueAnswer -> {
            tags.updateWithCheckDate("smoothness", value.osmValue)
        }
        is WrongSurfaceAnswer -> {
            tags.remove("surface")
            tags.remove("smoothness")
            tags.removeCheckDatesForKey("smoothness")
        }
        is IsActuallyStepsAnswer -> {
            tags.changeToSteps()
        }
    }
}
