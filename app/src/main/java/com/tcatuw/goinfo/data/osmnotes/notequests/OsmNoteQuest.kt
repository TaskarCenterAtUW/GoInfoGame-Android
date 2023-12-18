package com.tcatuw.goinfo.data.osmnotes.notequests

import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.geometry.ElementPointGeometry
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.quest.OsmNoteQuestKey
import com.tcatuw.goinfo.data.quest.Quest
import com.tcatuw.goinfo.data.quest.QuestType

/** Represents one task for the user to contribute to a public OSM note */
data class OsmNoteQuest(
    val id: Long,
    override val position: LatLon
) : Quest {
    override val type: QuestType get() = OsmNoteQuestType
    override val key: OsmNoteQuestKey by lazy { OsmNoteQuestKey(id) }
    override val markerLocations: Collection<LatLon> by lazy { listOf(position) }
    override val geometry: ElementGeometry get() = ElementPointGeometry(position)
}
