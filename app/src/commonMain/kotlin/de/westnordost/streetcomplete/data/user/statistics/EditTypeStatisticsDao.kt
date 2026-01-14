package de.westnordost.streetcomplete.data.user.statistics

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.statistics.EditTypeStatisticsTable.Columns.ELEMENT_EDIT_TYPE
import de.westnordost.streetcomplete.data.user.statistics.EditTypeStatisticsTable.Columns.SUCCEEDED

/** Stores how many edits of which element type the user did */
class EditTypeStatisticsDao(
    private val db: Database, private val name: String,
    private val preferences: Preferences? = null,
) {
    private val workspaceId
        get() = preferences?.workspaceId ?: 0
    fun getTotalAmount(): Int =
        db.queryOne(name, arrayOf("total($SUCCEEDED) as count"), where = "$WORKSPACE_ID = $workspaceId") { it.getInt("count") } ?: 0

    fun getAll(): List<EditTypeStatistics> =
        db.query(name, where = "$WORKSPACE_ID = $workspaceId") { it.toEditTypeStatistics() }

    fun clear() {
        db.delete(name, where = "$WORKSPACE_ID = $workspaceId")
    }

    fun replaceAll(amounts: Map<String, Int>) {
        db.transaction {
            db.delete(name, where = "$WORKSPACE_ID = $workspaceId")
            if (amounts.isNotEmpty()) {
                db.replaceMany(name,
                    arrayOf(ELEMENT_EDIT_TYPE, SUCCEEDED, WORKSPACE_ID),
                    amounts.map { arrayOf(it.key, it.value, workspaceId)
                    }
                )
            }
        }
    }

    fun addOne(type: String) {
        db.transaction {
            // first ensure the row exists
            db.insertOrIgnore(name, listOf(
                ELEMENT_EDIT_TYPE to type,
                SUCCEEDED to 0,
                WORKSPACE_ID to workspaceId
            ))

            // then increase by one
            db.exec("UPDATE $name SET $SUCCEEDED = $SUCCEEDED + 1 WHERE $ELEMENT_EDIT_TYPE = ? AND $WORKSPACE_ID = $workspaceId", arrayOf(type))
        }
    }

    fun subtractOne(type: String) {
        db.exec("UPDATE $name SET $SUCCEEDED = $SUCCEEDED - 1 WHERE $ELEMENT_EDIT_TYPE = ? AND $WORKSPACE_ID = $workspaceId", arrayOf(type))
    }

    fun getAmount(type: String): Int =
        db.queryOne(name,
            columns = arrayOf(SUCCEEDED),
            where = "$ELEMENT_EDIT_TYPE = ? AND $WORKSPACE_ID = $workspaceId",
            args = arrayOf(type)
        ) { it.getInt(SUCCEEDED) } ?: 0

    fun getAmount(type: List<String>): Int {
        val questionMarks = Array(type.size) { "?" }.joinToString(",")
        return db.queryOne(name,
            columns = arrayOf("total($SUCCEEDED) as count"),
            where = "$ELEMENT_EDIT_TYPE in ($questionMarks) AND $WORKSPACE_ID = $workspaceId",
            args = type.toTypedArray()
        ) { it.getInt("count") } ?: 0
    }
}

private fun CursorPosition.toEditTypeStatistics() = EditTypeStatistics(
    getString(ELEMENT_EDIT_TYPE),
    getInt(SUCCEEDED)
)
