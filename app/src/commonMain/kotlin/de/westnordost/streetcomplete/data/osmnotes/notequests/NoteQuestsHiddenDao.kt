package de.westnordost.streetcomplete.data.osmnotes.notequests

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.Columns.NOTE_ID
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.Columns.TIMESTAMP
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds

/** Persists which note ids should be hidden (because the user selected so) in the note quest */
class NoteQuestsHiddenDao(private val db: Database, private val preferences: Preferences? = null) {

    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(noteId: Long) {
        db.insert(
            NAME, listOf(
                NOTE_ID to noteId,
                TIMESTAMP to nowAsEpochMilliseconds(),
                WORKSPACE_ID to workspaceId
            )
        )
    }

    fun getTimestamp(noteId: Long): Long? =
        db.queryOne(
            NAME,
            where = "$NOTE_ID = $noteId AND $WORKSPACE_ID = $workspaceId"
        ) { it.getLong(TIMESTAMP) }

    fun delete(noteId: Long): Boolean =
        db.delete(NAME, where = "$NOTE_ID = $noteId AND $WORKSPACE_ID = $workspaceId") == 1

    fun getNewerThan(timestamp: Long): List<NoteQuestHiddenAt> =
        db.query(
            NAME,
            where = "$TIMESTAMP > $timestamp AND $WORKSPACE_ID = $workspaceId"
        ) { it.toNoteQuestHiddenAt() }

    fun getAll(): List<NoteQuestHiddenAt> =
        db.query(NAME, where = "$WORKSPACE_ID = $workspaceId") { it.toNoteQuestHiddenAt() }

    fun deleteAll(): Int =
        db.delete(NAME, where = "$WORKSPACE_ID = $workspaceId")

    fun countAll(): Int =
        db.queryOne(
            NAME, where = "$WORKSPACE_ID = $workspaceId",
            columns = arrayOf("COUNT(*)")
        ) { it.getInt("COUNT(*)") } ?: 0
}

private fun CursorPosition.toNoteQuestHiddenAt() =
    NoteQuestHiddenAt(
        getLong(NOTE_ID),
        getLong(TIMESTAMP), getInt(WORKSPACE_ID)
    )

data class NoteQuestHiddenAt(val noteId: Long, val timestamp: Long, val workspaceId: Int)
