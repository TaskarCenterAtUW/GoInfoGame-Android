package com.tcatuw.goinfo.util.ktx

import android.location.Location
import com.tcatuw.goinfo.data.osm.mapdata.LatLon

fun Location.toLatLon() = LatLon(latitude, longitude)
