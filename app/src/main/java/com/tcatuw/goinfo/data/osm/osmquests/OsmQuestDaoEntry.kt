package com.tcatuw.goinfo.data.osm.osmquests

import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.quest.OsmQuestKey

interface OsmQuestDaoEntry {
    val questTypeName: String
    val elementType: ElementType
    val elementId: Long
    val position: LatLon
}

val OsmQuestDaoEntry.key get() =
    OsmQuestKey(elementType, elementId, questTypeName)
