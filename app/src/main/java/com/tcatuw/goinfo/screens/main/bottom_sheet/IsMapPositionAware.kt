package com.tcatuw.goinfo.screens.main.bottom_sheet

import com.tcatuw.goinfo.data.osm.mapdata.LatLon

interface IsMapPositionAware {
    fun onMapMoved(position: LatLon)
}
