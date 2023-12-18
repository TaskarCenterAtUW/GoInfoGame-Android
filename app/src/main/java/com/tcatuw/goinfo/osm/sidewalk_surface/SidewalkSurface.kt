package com.tcatuw.goinfo.osm.sidewalk_surface

import com.tcatuw.goinfo.osm.surface.SurfaceAndNote

data class LeftAndRightSidewalkSurface(
    val left: SurfaceAndNote?,
    val right: SurfaceAndNote?,
)
