package com.tcatuw.goinfo.data.osm.created_elements

import com.tcatuw.goinfo.data.CursorPosition
import com.tcatuw.goinfo.data.Database
import com.tcatuw.goinfo.data.osm.created_elements.CreatedElementsTable.Columns.ELEMENT_ID
import com.tcatuw.goinfo.data.osm.created_elements.CreatedElementsTable.Columns.ELEMENT_TYPE
import com.tcatuw.goinfo.data.osm.created_elements.CreatedElementsTable.NAME
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.ElementType

/** Persists the keys of the elements created by this app already uploaded to the OSM API */
class CreatedElementsDao(private val db: Database) {

    fun putAll(entries: Collection<ElementKey>) {
        if (entries.isEmpty()) return

        db.replaceMany(
            NAME,
            arrayOf(ELEMENT_TYPE, ELEMENT_ID),
            entries.map { arrayOf(it.type.name, it.id) }
        )
    }

    fun getAll(): List<ElementKey> =
        db.query(NAME) { it.toElementKey() }

    fun deleteAll(entries: Collection<ElementKey>) {
        if (entries.isEmpty()) return
        db.transaction {
            for (entry in entries) {
                db.delete(
                    NAME,
                    where = "$ELEMENT_TYPE = ? AND $ELEMENT_ID = ?",
                    args = arrayOf(entry.type.name, entry.id)
                )
            }
        }
    }

    fun clear() {
        db.delete(NAME)
    }
}

private fun CursorPosition.toElementKey() = ElementKey(
    ElementType.valueOf(getString(ELEMENT_TYPE)),
    getLong(ELEMENT_ID)
)
