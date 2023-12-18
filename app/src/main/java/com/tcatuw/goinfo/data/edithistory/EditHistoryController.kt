package com.tcatuw.goinfo.data.edithistory

import com.tcatuw.goinfo.ApplicationConstants.MAX_UNDO_HISTORY_AGE
import com.tcatuw.goinfo.data.osm.edits.ElementEdit
import com.tcatuw.goinfo.data.osm.edits.ElementEditsController
import com.tcatuw.goinfo.data.osm.edits.ElementEditsSource
import com.tcatuw.goinfo.data.osm.edits.IsRevertAction
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestController
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestHidden
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEdit
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditsController
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditsSource
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestController
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestHidden
import com.tcatuw.goinfo.util.Listeners
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds

/** All edits done by the user in one place: Edits made on notes, on map data, hidings of quests */
class EditHistoryController(
    private val elementEditsController: ElementEditsController,
    private val noteEditsController: NoteEditsController,
    private val noteQuestController: OsmNoteQuestController,
    private val osmQuestController: OsmQuestController
) : EditHistorySource {
    private val listeners = Listeners<EditHistorySource.Listener>()

    private val osmElementEditsListener = object : ElementEditsSource.Listener {
        override fun onAddedEdit(edit: ElementEdit) {
            if (edit.action !is IsRevertAction) onAdded(edit)
        }
        override fun onSyncedEdit(edit: ElementEdit) {
            if (edit.action !is IsRevertAction) onSynced(edit)
        }
        override fun onDeletedEdits(edits: List<ElementEdit>) {
            onDeleted(edits.filter { it.action !is IsRevertAction })
        }
    }

    private val osmNoteEditsListener = object : NoteEditsSource.Listener {
        override fun onAddedEdit(edit: NoteEdit) { onAdded(edit) }
        override fun onSyncedEdit(edit: NoteEdit) { onSynced(edit) }
        override fun onDeletedEdits(edits: List<NoteEdit>) { onDeleted(edits) }
    }

    private val osmNoteQuestHiddenListener = object : OsmNoteQuestController.HideOsmNoteQuestListener {
        override fun onHid(edit: OsmNoteQuestHidden) { onAdded(edit) }
        override fun onUnhid(edit: OsmNoteQuestHidden) { onDeleted(listOf(edit)) }
        override fun onUnhidAll() { onInvalidated() }
    }
    private val osmQuestHiddenListener = object : OsmQuestController.HideOsmQuestListener {
        override fun onHid(edit: OsmQuestHidden) { onAdded(edit) }
        override fun onUnhid(edit: OsmQuestHidden) { onDeleted(listOf(edit)) }
        override fun onUnhidAll() { onInvalidated() }
    }

    init {
        elementEditsController.addListener(osmElementEditsListener)
        noteEditsController.addListener(osmNoteEditsListener)
        noteQuestController.addHideQuestsListener(osmNoteQuestHiddenListener)
        osmQuestController.addHideQuestsListener(osmQuestHiddenListener)
    }

    fun undo(edit: Edit): Boolean {
        if (!edit.isUndoable) return false
        return when (edit) {
            is ElementEdit -> elementEditsController.undo(edit)
            is NoteEdit -> noteEditsController.undo(edit)
            is OsmNoteQuestHidden -> noteQuestController.unhide(edit.note.id)
            is OsmQuestHidden -> osmQuestController.unhide(edit.questKey)
            else -> throw IllegalArgumentException()
        }
    }

    fun deleteSyncedOlderThan(timestamp: Long): Int =
        elementEditsController.deleteSyncedOlderThan(timestamp) +
        noteEditsController.deleteSyncedOlderThan(timestamp)

    override fun get(key: EditKey): Edit? = when (key) {
        is ElementEditKey -> elementEditsController.get(key.id)
        is NoteEditKey -> noteEditsController.get(key.id)
        is OsmNoteQuestHiddenKey -> noteQuestController.getHidden(key.osmNoteQuestKey.noteId)
        is OsmQuestHiddenKey -> osmQuestController.getHidden(key.osmQuestKey)
    }

    override fun getMostRecentUndoable(): Edit? =
        // this could be optimized later by not querying all. Though, the amount that is queried
        // from database should never be that big anyway...
        getAll().firstOrNull { it.isUndoable }

    override fun getAll(): List<Edit> {
        val maxAge = nowAsEpochMilliseconds() - MAX_UNDO_HISTORY_AGE

        val result = ArrayList<Edit>()
        result += elementEditsController.getAll().filter { it.action !is IsRevertAction }
        result += noteEditsController.getAll()
        result += noteQuestController.getAllHiddenNewerThan(maxAge)
        result += osmQuestController.getAllHiddenNewerThan(maxAge)

        result.sortByDescending { it.createdTimestamp }
        return result
    }

    override fun getCount(): Int =
        // could be optimized later too...
        getAll().size

    override fun addListener(listener: EditHistorySource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: EditHistorySource.Listener) {
        listeners.remove(listener)
    }

    private fun onAdded(edit: Edit) {
        listeners.forEach { it.onAdded(edit) }
    }
    private fun onSynced(edit: Edit) {
        listeners.forEach { it.onSynced(edit) }
    }
    private fun onDeleted(edits: List<Edit>) {
        listeners.forEach { it.onDeleted(edits) }
    }
    private fun onInvalidated() {
        listeners.forEach { it.onInvalidated() }
    }
}
