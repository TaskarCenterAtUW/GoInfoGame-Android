package com.tcatuw.goinfo.data.download.strategy

import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox
import com.tcatuw.goinfo.data.osm.mapdata.LatLon

interface AutoDownloadStrategy {
    /** returns the bbox that should be downloaded at this position or null if nothing should be
     *  downloaded now */
    suspend fun getDownloadBoundingBox(pos: LatLon): BoundingBox?
}
