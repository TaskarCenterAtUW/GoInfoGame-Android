package com.tcatuw.goinfo.util.math

import com.tcatuw.goinfo.data.osm.geometry.ElementPolylinesGeometry

fun ElementPolylinesGeometry.getOrientationAtCenterLineInDegrees(): Float {
    val centerLine = polylines.first().centerLineOfPolyline()
    return centerLine.first.initialBearingTo(centerLine.second).toFloat()
}

fun ElementPolylinesGeometry.intersects(other: ElementPolylinesGeometry): Boolean =
    getBounds().intersect(other.getBounds())
    && polylines.any { polyline ->
        other.polylines.any { otherPolyline ->
            polyline.intersectsWith(otherPolyline)
        }
    }
