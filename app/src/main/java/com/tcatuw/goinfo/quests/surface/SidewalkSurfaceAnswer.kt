package com.tcatuw.goinfo.quests.surface

import com.tcatuw.goinfo.osm.sidewalk_surface.LeftAndRightSidewalkSurface

sealed interface SidewalkSurfaceAnswer

data class SidewalkSurface(val value: LeftAndRightSidewalkSurface) : SidewalkSurfaceAnswer
object SidewalkIsDifferent : SidewalkSurfaceAnswer
