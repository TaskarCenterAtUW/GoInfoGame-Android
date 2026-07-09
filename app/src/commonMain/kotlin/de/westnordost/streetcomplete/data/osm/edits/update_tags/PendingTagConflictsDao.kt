package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.AllEditTypes
import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.ElementEditType
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.CREATED_TIMESTAMP
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.EDIT_ID
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.ELEMENT_ID
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.ELEMENT_TYPE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.ID
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.LATITUDE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.LONGITUDE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.MINE_VALUE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.QUEST_TYPE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.SOURCE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.THEIRS_VALUE
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable.NAME
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Preferences

class PendingTagConflictsDao(
    private val db: Database,
    private val allEditTypes: AllEditTypes,
    private val preferences: Preferences? = null,
) {
    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(conflict: PendingTagConflict): PendingTagConflict {
        val rowId = db.insert(NAME, conflict.toPairs())
        return conflict.copy(id = rowId, workspaceId = workspaceId)
    }

    fun getAll(): List<PendingTagConflict> =
        db.query(
            NAME,
            where = "$WORKSPACE_ID = $workspaceId",
            orderBy = CREATED_TIMESTAMP
        ) { it.toPendingTagConflict() }

    fun getCount(): Int =
        db.queryOne(
            NAME,
            columns = arrayOf("COUNT(*) AS count"),
            where = "$WORKSPACE_ID = $workspaceId"
        ) { it.getInt("count") } ?: 0

    fun delete(id: Long): Boolean =
        db.delete(NAME, "$WORKSPACE_ID = $workspaceId AND $ID = $id") == 1

    private fun PendingTagConflict.toPairs(): List<Pair<String, Any?>> = listOf(
        EDIT_ID to editId,
        ELEMENT_TYPE to elementType.name,
        ELEMENT_ID to elementId,
        PendingTagConflictsTable.Columns.TAG_KEY to tagKey,
        MINE_VALUE to mineValue,
        THEIRS_VALUE to theirsValueAtDetection,
        QUEST_TYPE to editType.name,
        SOURCE to source,
        LATITUDE to position.latitude,
        LONGITUDE to position.longitude,
        CREATED_TIMESTAMP to createdTimestamp,
        WORKSPACE_ID to workspaceId
    )

    private fun CursorPosition.toPendingTagConflict() = PendingTagConflict(
        id = getLong(ID),
        editId = getLong(EDIT_ID),
        elementType = ElementType.valueOf(getString(ELEMENT_TYPE)),
        elementId = getLong(ELEMENT_ID),
        tagKey = getString(PendingTagConflictsTable.Columns.TAG_KEY),
        mineValue = getStringOrNull(MINE_VALUE),
        theirsValueAtDetection = getStringOrNull(THEIRS_VALUE),
        editType = allEditTypes.getByName(getString(QUEST_TYPE)) as ElementEditType,
        source = getString(SOURCE),
        position = LatLon(getDouble(LATITUDE), getDouble(LONGITUDE)),
        createdTimestamp = getLong(CREATED_TIMESTAMP),
        workspaceId = getInt(WORKSPACE_ID)
    )
}
