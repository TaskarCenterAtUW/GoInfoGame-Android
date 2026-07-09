package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.data.AllEditTypes
import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.CREATED_TIMESTAMP
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.ELEMENT_ID
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.ELEMENT_TYPE
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.ID
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.LATITUDE
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.LONGITUDE
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.QUEST_TYPE
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.REASON
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable.NAME
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Preferences

class DiscardedEditNoticesDao(
    private val db: Database,
    private val allEditTypes: AllEditTypes,
    private val preferences: Preferences? = null,
) {
    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(notice: DiscardedEditNotice): DiscardedEditNotice {
        val rowId = db.insert(NAME, notice.toPairs())
        return notice.copy(id = rowId, workspaceId = workspaceId)
    }

    fun getAll(): List<DiscardedEditNotice> =
        db.query(
            NAME,
            where = "$WORKSPACE_ID = $workspaceId",
            orderBy = CREATED_TIMESTAMP
        ) { it.toDiscardedEditNotice() }

    fun getCount(): Int =
        db.queryOne(
            NAME,
            columns = arrayOf("COUNT(*) AS count"),
            where = "$WORKSPACE_ID = $workspaceId"
        ) { it.getInt("count") } ?: 0

    fun delete(id: Long): Boolean =
        db.delete(NAME, "$WORKSPACE_ID = $workspaceId AND $ID = $id") == 1

    private fun DiscardedEditNotice.toPairs(): List<Pair<String, Any?>> = listOf(
        QUEST_TYPE to editType.name,
        ELEMENT_TYPE to elementType?.name,
        ELEMENT_ID to elementId,
        REASON to reason,
        LATITUDE to position.latitude,
        LONGITUDE to position.longitude,
        CREATED_TIMESTAMP to createdTimestamp,
        WORKSPACE_ID to workspaceId
    )

    private fun CursorPosition.toDiscardedEditNotice() = DiscardedEditNotice(
        id = getLong(ID),
        editType = allEditTypes.getByName(getString(QUEST_TYPE)) as ElementEditType,
        elementType = getStringOrNull(ELEMENT_TYPE)?.let { ElementType.valueOf(it) },
        elementId = getLongOrNull(ELEMENT_ID),
        reason = getString(REASON),
        position = LatLon(getDouble(LATITUDE), getDouble(LONGITUDE)),
        createdTimestamp = getLong(CREATED_TIMESTAMP),
        workspaceId = getInt(WORKSPACE_ID)
    )
}
