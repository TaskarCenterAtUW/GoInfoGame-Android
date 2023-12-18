package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.osm.surface.SurfaceAndNote

sealed interface SurfaceOrIsStepsAnswer
object IsActuallyStepsAnswer : SurfaceOrIsStepsAnswer
object IsIndoorsAnswer : SurfaceOrIsStepsAnswer
data class SurfaceAnswer(val value: SurfaceAndNote) : SurfaceOrIsStepsAnswer
