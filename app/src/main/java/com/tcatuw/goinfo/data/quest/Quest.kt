package com.tcatuw.goinfo.data.quest

import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.mapdata.LatLon

/** Represents one task for the user to complete/correct  */
interface Quest {
    val key: QuestKey
    val position: LatLon
    val markerLocations: Collection<LatLon>
    val geometry: ElementGeometry

    val type: QuestType
}
