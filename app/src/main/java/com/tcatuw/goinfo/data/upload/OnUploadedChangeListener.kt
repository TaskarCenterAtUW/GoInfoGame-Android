package com.tcatuw.goinfo.data.upload

import com.tcatuw.goinfo.data.osm.mapdata.LatLon

interface OnUploadedChangeListener {
    fun onUploaded(questType: String, at: LatLon)
    fun onDiscarded(questType: String, at: LatLon)
}
