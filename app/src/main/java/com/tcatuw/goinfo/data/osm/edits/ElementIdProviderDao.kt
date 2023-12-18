package com.tcatuw.goinfo.data.osm.edits

import com.tcatuw.goinfo.data.CursorPosition
import com.tcatuw.goinfo.data.Database
import com.tcatuw.goinfo.data.osm.edits.ElementIdProviderTable.Columns.EDIT_ID
import com.tcatuw.goinfo.data.osm.edits.ElementIdProviderTable.Columns.ELEMENT_ID
import com.tcatuw.goinfo.data.osm.edits.ElementIdProviderTable.Columns.ELEMENT_TYPE
import com.tcatuw.goinfo.data.osm.edits.ElementIdProviderTable.NAME
import com.tcatuw.goinfo.data.osm.mapdata.ElementIdUpdate
import com.tcatuw.goinfo.data.osm.mapdata.ElementKey
import com.tcatuw.goinfo.data.osm.mapdata.ElementType

/** Assigns new element ids for ElementEditActions that create new elements */
class ElementIdProviderDao(private val db: Database) {

    fun assign(editId: Long, nodeCount: Int, wayCount: Int, relationCount: Int) {
        if (nodeCount == 0 && wayCount == 0 && relationCount == 0) return

        db.insertMany(NAME,
            arrayOf(EDIT_ID, ELEMENT_TYPE),
            sequence {
                repeat(nodeCount) { yield(ElementType.NODE) }
                repeat(wayCount) { yield(ElementType.WAY) }
                repeat(relationCount) { yield(ElementType.RELATION) }
            }.map { arrayOf<Any?>(editId, it.name) }.asIterable()
        )
    }

    fun updateIds(updates: Collection<ElementIdUpdate>) {
        if (updates.isEmpty()) return
        // the ids in the table are actually negated because of autoincrement
        db.transaction {
            for (update in updates) {
                db.update(
                    table = NAME,
                    values = listOf(
                        ELEMENT_TYPE to update.elementType.name,
                        ELEMENT_ID to -update.newElementId
                    ),
                    where = "$ELEMENT_TYPE = ? AND $ELEMENT_ID = ?",
                    args = arrayOf(
                        update.elementType.name,
                        -update.oldElementId
                    )
                )
            }
        }
    }

    fun get(editId: Long) = ElementIdProvider(
        db.query(NAME, where = "$EDIT_ID = $editId") { it.toElementKey() }
    )

    fun delete(editId: Long): Int =
        db.delete(NAME, "$EDIT_ID = $editId")

    fun deleteAll(editIds: List<Long>): Int =
        db.delete(NAME, "$EDIT_ID IN (${editIds.joinToString(",")})")
}

private fun CursorPosition.toElementKey() = ElementKey(
    ElementType.valueOf(getString(ELEMENT_TYPE)),
    -getLong(ELEMENT_ID)
)
