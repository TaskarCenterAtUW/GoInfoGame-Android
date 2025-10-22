package de.westnordost.streetcomplete.data.osm.osmquests

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.ELEMENT_ID
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.ELEMENT_TYPE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.LATITUDE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.LONGITUDE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.QUEST_TYPE
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.queryIn
import de.westnordost.streetcomplete.data.quest.OsmQuestKey

/** Persists OsmQuest objects, or more specifically, OsmQuestEntry objects */
class OsmQuestDao(private val db: Database, val preferences: Preferences) {

    private val workspaceId
        get() = preferences.workspaceId ?: 0

    fun put(quest: OsmQuestDaoEntry) {
        db.replace(NAME, quest.toPairs())
    }

    fun get(key: OsmQuestKey): OsmQuestDaoEntry? =
        db.queryOne(
            NAME,
            where = "$ELEMENT_TYPE = ? AND $ELEMENT_ID = ? AND $QUEST_TYPE = ? AND $WORKSPACE_ID = ?",
            args = arrayOf(key.elementType.name, key.elementId, key.questTypeName, workspaceId)
        ) { it.toOsmQuestEntry() }

    fun delete(key: OsmQuestKey): Boolean =
        db.delete(
            NAME,
            where = "$ELEMENT_TYPE = ? AND $ELEMENT_ID = ? AND $QUEST_TYPE = ? AND $WORKSPACE_ID = ?",
            args = arrayOf(key.elementType.name, key.elementId, key.questTypeName, workspaceId)
        ) == 1

    fun putAll(quests: Collection<OsmQuestDaoEntry>) {
        if (quests.isEmpty()) return
        // replace because even if the quest already exists in DB, the center position might have changed
        db.replaceMany(
            NAME,
            arrayOf(QUEST_TYPE, ELEMENT_TYPE, ELEMENT_ID, LATITUDE, LONGITUDE, WORKSPACE_ID),
            quests.map {
                arrayOf(
                    it.questTypeName,
                    it.elementType.name,
                    it.elementId,
                    it.position.latitude,
                    it.position.longitude,
                    workspaceId
                )
            }
        )
    }

    fun getAllForElements(keys: Collection<ElementKey>): List<OsmQuestDaoEntry> {
        if (keys.isEmpty()) return emptyList()
        return db.queryIn(
            NAME,
            whereColumns = arrayOf(ELEMENT_TYPE, ELEMENT_ID, WORKSPACE_ID),
            whereArgs = keys.map { arrayOf(it.type.name, it.id, workspaceId) }
        ) { it.toOsmQuestEntry() }
    }

    fun getAllInBBox(
        bounds: BoundingBox,
        questTypes: Collection<String>? = null
    ): List<OsmQuestDaoEntry> {
        var builder = inBoundsSql(bounds, workspaceId)
        if (questTypes != null) {
            if (questTypes.isEmpty()) return emptyList()
            val questTypesStr = questTypes.joinToString(",") { "'$it'" }
            builder += " AND $QUEST_TYPE IN ($questTypesStr)"
        }
        return db.query(NAME, where = builder) { it.toOsmQuestEntry() }
    }

    fun deleteAll(keys: Collection<OsmQuestKey>) {
        if (keys.isEmpty()) return
        db.transaction {
            for (key in keys) {
                delete(key)
            }
        }
    }

    fun clear() {
        db.delete(NAME)
    }
}

private fun inBoundsSql(bbox: BoundingBox, workspaceId: Int): String = """
    ($LATITUDE BETWEEN ${bbox.min.latitude} AND ${bbox.max.latitude}) AND
    ($LONGITUDE BETWEEN ${bbox.min.longitude} AND ${bbox.max.longitude}) AND $WORKSPACE_ID = $workspaceId
""".trimIndent()

private fun CursorPosition.toOsmQuestEntry(): OsmQuestDaoEntry = BasicOsmQuestDaoEntry(
    ElementType.valueOf(getString(ELEMENT_TYPE)),
    getLong(ELEMENT_ID),
    getString(QUEST_TYPE),
    LatLon(getDouble(LATITUDE), getDouble(LONGITUDE)),
    getInt(WORKSPACE_ID)
)

private fun OsmQuestDaoEntry.toPairs() = listOf(
    QUEST_TYPE to questTypeName,
    ELEMENT_TYPE to elementType.name,
    ELEMENT_ID to elementId,
    LATITUDE to position.latitude,
    LONGITUDE to position.longitude,
    WORKSPACE_ID to workspaceId
)

data class BasicOsmQuestDaoEntry(
    override val elementType: ElementType,
    override val elementId: Long,
    override val questTypeName: String,
    override val position: LatLon,
    override val workspaceId: Int
) : OsmQuestDaoEntry
