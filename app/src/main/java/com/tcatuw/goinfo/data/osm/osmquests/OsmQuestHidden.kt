package com.tcatuw.goinfo.data.osm.osmquests

import com.tcatuw.goinfo.data.edithistory.Edit
import com.tcatuw.goinfo.data.edithistory.OsmQuestHiddenKey
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.quest.OsmQuestKey

data class OsmQuestHidden(
    val elementType: ElementType,
    val elementId: Long,
    val questType: OsmElementQuestType<*>,
    override val position: LatLon,
    override val createdTimestamp: Long
) : Edit {
    val questKey get() = OsmQuestKey(elementType, elementId, questType.name)
    override val key: OsmQuestHiddenKey get() = OsmQuestHiddenKey(questKey)
    override val isUndoable: Boolean get() = true
    override val isSynced: Boolean? get() = null
}
