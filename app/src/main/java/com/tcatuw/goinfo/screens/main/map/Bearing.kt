package com.tcatuw.goinfo.screens.main.map

import com.tcatuw.goinfo.data.osmtracks.Trackpoint
import com.tcatuw.goinfo.util.math.distanceTo
import com.tcatuw.goinfo.util.math.initialBearingTo

/** Utility function to estimate current bearing from a track */
fun getTrackBearing(track: List<Trackpoint>): Double? {
    val last = track.lastOrNull()?.position ?: return null
    val point = track.findLast { it.position.distanceTo(last) > MIN_TRACK_DISTANCE_FOR_BEARING }?.position ?: return null
    return point.initialBearingTo(last)
}

private const val MIN_TRACK_DISTANCE_FOR_BEARING = 15f // 15 meters
