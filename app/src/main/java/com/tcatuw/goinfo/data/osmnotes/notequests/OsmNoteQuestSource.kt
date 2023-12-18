package com.tcatuw.goinfo.data.osmnotes.notequests

import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox

interface OsmNoteQuestSource {
    interface Listener {
        fun onUpdated(addedQuests: Collection<OsmNoteQuest>, deletedQuestIds: Collection<Long>)
        fun onInvalidated()
    }

    /** get single quest by id if not hidden by user */
    fun getVisible(questId: Long): OsmNoteQuest?

    /** Get all quests in given bounding box */
    fun getAllVisibleInBBox(bbox: BoundingBox): List<OsmNoteQuest>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
