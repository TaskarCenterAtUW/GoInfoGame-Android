package com.tcatuw.goinfo.data.osm.osmquests

import com.tcatuw.goinfo.data.CursorPosition
import com.tcatuw.goinfo.data.Database
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestsHiddenTable.Columns.ELEMENT_ID
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestsHiddenTable.Columns.ELEMENT_TYPE
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestsHiddenTable.Columns.QUEST_TYPE
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestsHiddenTable.Columns.TIMESTAMP
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestsHiddenTable.NAME
import com.tcatuw.goinfo.data.quest.OsmQuestKey
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds

/** Persists which osm quests should be hidden (because the user selected so) */
class OsmQuestsHiddenDao(private val db: Database) {

    fun add(osmQuestKey: OsmQuestKey) {
        db.insert(NAME, osmQuestKey.toPairs())
    }

    fun contains(osmQuestKey: OsmQuestKey): Boolean =
        getTimestamp(osmQuestKey) != null

    fun getTimestamp(osmQuestKey: OsmQuestKey): Long? =
        db.queryOne(NAME,
            where = "$QUEST_TYPE = ? AND $ELEMENT_ID = ? AND $ELEMENT_TYPE = ?",
            args = arrayOf(
                osmQuestKey.questTypeName,
                osmQuestKey.elementId,
                osmQuestKey.elementType.name,
            )
        ) { it.getLong(TIMESTAMP) }

    fun delete(osmQuestKey: OsmQuestKey): Boolean =
        db.delete(NAME,
            where = "$QUEST_TYPE = ? AND $ELEMENT_ID = ? AND $ELEMENT_TYPE = ?",
            args = arrayOf(
                osmQuestKey.questTypeName,
                osmQuestKey.elementId,
                osmQuestKey.elementType.name,
            )
        ) == 1

    fun getNewerThan(timestamp: Long): List<OsmQuestKeyWithTimestamp> =
        db.query(NAME, where = "$TIMESTAMP > $timestamp") { it.toHiddenOsmQuest() }

    fun getAllIds(): List<OsmQuestKey> =
        db.query(NAME) { it.toOsmQuestKey() }

    fun deleteAll(): Int =
        db.delete(NAME)
}

private fun OsmQuestKey.toPairs() = listOf(
    ELEMENT_TYPE to elementType.name,
    ELEMENT_ID to elementId,
    QUEST_TYPE to questTypeName,
    TIMESTAMP to nowAsEpochMilliseconds()
)

private fun CursorPosition.toOsmQuestKey() = OsmQuestKey(
    ElementType.valueOf(getString(ELEMENT_TYPE)),
    getLong(ELEMENT_ID),
    getString(QUEST_TYPE)
)

private fun CursorPosition.toHiddenOsmQuest() = OsmQuestKeyWithTimestamp(toOsmQuestKey(), getLong(TIMESTAMP))

data class OsmQuestKeyWithTimestamp(val osmQuestKey: OsmQuestKey, val timestamp: Long)
