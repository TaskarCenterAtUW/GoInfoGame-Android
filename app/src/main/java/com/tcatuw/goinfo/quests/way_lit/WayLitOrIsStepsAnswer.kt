package com.tcatuw.goinfo.quests.way_lit

import com.tcatuw.goinfo.osm.lit.LitStatus

sealed interface WayLitOrIsStepsAnswer
object IsActuallyStepsAnswer : WayLitOrIsStepsAnswer
data class WayLit(val litStatus: LitStatus) : WayLitOrIsStepsAnswer
