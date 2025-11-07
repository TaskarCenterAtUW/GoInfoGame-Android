package de.westnordost.streetcomplete.data.osmnotes.edits

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.CREATED_TIMESTAMP
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.ID
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.IMAGES_NEED_ACTIVATION
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.IMAGE_PATHS
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.IS_SYNCED
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.LATITUDE
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.LONGITUDE
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.NOTE_ID
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.TEXT
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.TRACK
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.TYPE
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NoteEditsDao(private val db: Database, private val preferences: Preferences) {

    private val workspaceId
        get() = preferences.workspaceId ?: 0

    fun add(edit: NoteEdit): Boolean =
        db.transaction {
            edit.workspaceId = workspaceId
            val rowId = db.insert(NAME, edit.toPairs())
            if (rowId == -1L) return@transaction false
            edit.id = rowId
            // if the note id is not set, set it to the negative of the row id
            if (edit.noteId == 0L) {
                db.update(NAME, listOf(NOTE_ID to -rowId), "$ID = $rowId AND $WORKSPACE_ID = $workspaceId")
                edit.noteId = -rowId
            }
            return@transaction true
        }

    fun get(id: Long): NoteEdit? =
        db.queryOne(NAME, where = "$ID = $id AND $WORKSPACE_ID = $workspaceId") { it.toNoteEdit() }

    fun getAll(): List<NoteEdit> =
        db.query(NAME,where = "$WORKSPACE_ID = $workspaceId", orderBy = "$IS_SYNCED, $CREATED_TIMESTAMP") { it.toNoteEdit() }

    fun getOldestUnsynced(): NoteEdit? =
        db.queryOne(NAME,
            where = "$IS_SYNCED = 0 AND $WORKSPACE_ID = $workspaceId",
            orderBy = CREATED_TIMESTAMP
        ) { it.toNoteEdit() }

    fun getUnsyncedCount(): Int =
        db.queryOne(NAME,
            columns = arrayOf("COUNT(*) as count"),
            where = "$IS_SYNCED = 0 AND $WORKSPACE_ID = $workspaceId"
        ) { it.getInt("count") } ?: 0

    fun getAllUnsynced(): List<NoteEdit> =
        db.query(NAME,
            where = "$IS_SYNCED = 0 AND $WORKSPACE_ID = $workspaceId",
            orderBy = CREATED_TIMESTAMP
        ) { it.toNoteEdit() }

    fun getAllUnsyncedForNote(noteId: Long): List<NoteEdit> =
        db.query(NAME,
            where = "$NOTE_ID = $noteId AND $WORKSPACE_ID = $workspaceId AND $IS_SYNCED = 0",
            orderBy = CREATED_TIMESTAMP
        ) { it.toNoteEdit() }

    fun getAllUnsyncedForNotes(noteIds: Collection<Long>): List<NoteEdit> {
        val notes = noteIds.joinToString(",")
        return db.query(NAME,
            where = "$NOTE_ID IN ($notes) AND $WORKSPACE_ID = $workspaceId AND $IS_SYNCED = 0",
            orderBy = CREATED_TIMESTAMP
        ) { it.toNoteEdit() }
    }

    fun getAllUnsynced(bbox: BoundingBox): List<NoteEdit> =
        db.query(NAME,
            where = "$IS_SYNCED = 0 AND $WORKSPACE_ID = $workspaceId AND " + inBoundsSql(bbox),
            orderBy = CREATED_TIMESTAMP
        ) { it.toNoteEdit() }

    fun getAllUnsyncedPositions(bbox: BoundingBox): List<LatLon> =
        db.query(NAME,
            columns = arrayOf(LATITUDE, LONGITUDE),
            where = "$IS_SYNCED = 0 AND $WORKSPACE_ID = $workspaceId AND " + inBoundsSql(bbox),
            orderBy = CREATED_TIMESTAMP
        ) { LatLon(it.getDouble(LATITUDE), it.getDouble(LONGITUDE)) }

    fun markSynced(id: Long): Boolean =
        db.update(NAME, listOf(IS_SYNCED to 1), "$ID = $id AND $WORKSPACE_ID = $workspaceId", null) == 1

    fun delete(id: Long): Boolean =
        db.delete(NAME, "$ID = $id AND $WORKSPACE_ID = $workspaceId") == 1

    fun deleteAll(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        return db.delete(NAME, "$ID IN (${ids.joinToString(",")}) AND $WORKSPACE_ID = $workspaceId")
    }

    fun getSyncedOlderThan(timestamp: Long): List<NoteEdit> =
        db.query(NAME, where = "$IS_SYNCED = 1 AND $IMAGES_NEED_ACTIVATION = 0 AND $CREATED_TIMESTAMP < $timestamp AND $WORKSPACE_ID = $workspaceId") { it.toNoteEdit() }

    fun updateNoteId(oldNoteId: Long, newNoteId: Long): Int =
        db.update(NAME, listOf(NOTE_ID to newNoteId), "$NOTE_ID = $oldNoteId AND $WORKSPACE_ID = $workspaceId")

    fun getOldestNeedingImagesActivation(): NoteEdit? =
        db.queryOne(NAME, where = "$IS_SYNCED = 1 AND $IMAGES_NEED_ACTIVATION = 1 AND $WORKSPACE_ID = $workspaceId", orderBy = CREATED_TIMESTAMP) { it.toNoteEdit() }

    fun markImagesActivated(id: Long): Boolean =
        db.update(NAME, listOf(IMAGES_NEED_ACTIVATION to 0), "$ID = $id AND $WORKSPACE_ID = $workspaceId") == 1

    fun replaceTextInUnsynced(text: String, replacement: String) {
        db.exec(
            "UPDATE $NAME SET $TEXT = replace($TEXT, ?, ?) WHERE $IS_SYNCED = 0 AND $WORKSPACE_ID = ?",
            arrayOf(text, replacement, workspaceId)
        )
    }

    private fun inBoundsSql(bbox: BoundingBox): String = """
        ($LATITUDE BETWEEN ${bbox.min.latitude} AND ${bbox.max.latitude}) AND
        ($LONGITUDE BETWEEN ${bbox.min.longitude} AND ${bbox.max.longitude}) AND
        ($WORKSPACE_ID = $workspaceId)
    """.trimIndent()

    private fun NoteEdit.toPairs() = listOf(
        NOTE_ID to noteId,
        LATITUDE to position.latitude,
        LONGITUDE to position.longitude,
        CREATED_TIMESTAMP to createdTimestamp,
        IS_SYNCED to if (isSynced) 1 else 0,
        TEXT to text,
        IMAGE_PATHS to Json.encodeToString(imagePaths),
        IMAGES_NEED_ACTIVATION to if (imagesNeedActivation) 1 else 0,
        TRACK to Json.encodeToString(track),
        TYPE to action.name,
        WORKSPACE_ID to workspaceId
    )

    private fun CursorPosition.toNoteEdit() = NoteEdit(
        getLong(ID),
        getLong(NOTE_ID),
        LatLon(getDouble(LATITUDE), getDouble(LONGITUDE)),
        NoteEditAction.valueOf(getString(TYPE)),
        getStringOrNull(TEXT),
        Json.decodeFromString(getString(IMAGE_PATHS)),
        getLong(CREATED_TIMESTAMP),
        getInt(IS_SYNCED) == 1,
        getInt(IMAGES_NEED_ACTIVATION) == 1,
        Json.decodeFromString(getString(TRACK)),
        getInt(WORKSPACE_ID)
    )
}
