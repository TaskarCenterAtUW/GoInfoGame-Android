package de.westnordost.streetcomplete.data.osm.osmquests

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.Columns.ELEMENT_ID
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.Columns.ELEMENT_TYPE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.Columns.QUEST_TYPE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.Columns.TIMESTAMP
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestsHiddenTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds

/** Persists which osm quests should be hidden (because the user selected so) */
class OsmQuestsHiddenDao(private val db: Database, val preferences: Preferences? = null) {

    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(osmQuestKey: OsmQuestKey) {
        osmQuestKey.workspaceId = workspaceId
        db.insert(NAME, osmQuestKey.toPairs())
    }

    fun getTimestamp(osmQuestKey: OsmQuestKey): Long? =
        db.queryOne(NAME,
            where = "$QUEST_TYPE = ? AND $ELEMENT_ID = ? AND $ELEMENT_TYPE = ? AND $WORKSPACE_ID = ?",
            args = arrayOf(
                osmQuestKey.questTypeName,
                osmQuestKey.elementId,
                osmQuestKey.elementType.name,
                workspaceId
            )
        ) { it.getLong(TIMESTAMP) }

    fun delete(osmQuestKey: OsmQuestKey): Boolean =
        db.delete(NAME,
            where = "$QUEST_TYPE = ? AND $ELEMENT_ID = ? AND $ELEMENT_TYPE = ? AND $WORKSPACE_ID = ?",
            args = arrayOf(
                osmQuestKey.questTypeName,
                osmQuestKey.elementId,
                osmQuestKey.elementType.name,
                workspaceId
            )
        ) == 1

    fun getNewerThan(timestamp: Long): List<OsmQuestHiddenAt> =
        db.query(NAME, where = "$TIMESTAMP > $timestamp AND $WORKSPACE_ID = $workspaceId") { it.toOsmQuestHiddenAt() }

    fun getAll(): List<OsmQuestHiddenAt> =
        db.query(NAME, where = "$WORKSPACE_ID = $workspaceId") { it.toOsmQuestHiddenAt() }

    fun deleteAll(): Int =
        db.delete(NAME, where = "$WORKSPACE_ID = $workspaceId")

    fun countAll(): Int =
        db.queryOne(NAME, columns = arrayOf("COUNT(*)"), where = "$WORKSPACE_ID = $workspaceId") { it.getInt("COUNT(*)") } ?: 0
}

private fun OsmQuestKey.toPairs() = listOf(
    ELEMENT_TYPE to elementType.name,
    ELEMENT_ID to elementId,
    QUEST_TYPE to questTypeName,
    TIMESTAMP to nowAsEpochMilliseconds(),
    WORKSPACE_ID to workspaceId
)

private fun CursorPosition.toOsmQuestKey() = OsmQuestKey(
    ElementType.valueOf(getString(ELEMENT_TYPE)),
    getLong(ELEMENT_ID),
    getString(QUEST_TYPE),
    getInt(WORKSPACE_ID)
)

private fun CursorPosition.toOsmQuestHiddenAt() = OsmQuestHiddenAt(
    toOsmQuestKey(),
    getLong(TIMESTAMP)
)

data class OsmQuestHiddenAt(val key: OsmQuestKey, val timestamp: Long)
