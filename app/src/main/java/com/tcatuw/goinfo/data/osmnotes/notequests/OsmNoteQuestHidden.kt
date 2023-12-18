package com.tcatuw.goinfo.data.osmnotes.notequests

import com.tcatuw.goinfo.data.edithistory.Edit
import com.tcatuw.goinfo.data.edithistory.OsmNoteQuestHiddenKey
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osmnotes.Note
import com.tcatuw.goinfo.data.quest.OsmNoteQuestKey

data class OsmNoteQuestHidden(
    val note: Note,
    override val createdTimestamp: Long
) : Edit {
    override val key: OsmNoteQuestHiddenKey get() = OsmNoteQuestHiddenKey(OsmNoteQuestKey(note.id))
    override val isUndoable: Boolean get() = true
    override val position: LatLon get() = note.position
    override val isSynced: Boolean? get() = null
}
