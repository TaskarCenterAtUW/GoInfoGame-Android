package com.tcatuw.goinfo.data.edithistory

import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.quest.OsmNoteQuestKey
import com.tcatuw.goinfo.data.quest.OsmQuestKey

interface Edit {
    val key: EditKey
    val createdTimestamp: Long
    val isUndoable: Boolean
    val position: LatLon
    val isSynced: Boolean?
}

sealed class EditKey

data class ElementEditKey(val id: Long) : EditKey()
data class NoteEditKey(val id: Long) : EditKey()
data class OsmQuestHiddenKey(val osmQuestKey: OsmQuestKey) : EditKey()
data class OsmNoteQuestHiddenKey(val osmNoteQuestKey: OsmNoteQuestKey) : EditKey()
