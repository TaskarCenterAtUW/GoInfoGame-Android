package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.EXTERNAL_APP_ACCESS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.ID
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.OVERRIDE_CONFLICTS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.QUESTS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.TITLE
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.TYPE
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.NAME

class WorkspaceDao(private val db: Database) {

    fun put(responseItems: List<Workspace>) {
        db.replaceMany(
            NAME,
            arrayOf(ID, TITLE, QUESTS, TYPE, EXTERNAL_APP_ACCESS, OVERRIDE_CONFLICTS),
            responseItems.map {
                arrayOf(it.id, it.title, it.quests?.joinToString(","), it.type, it.externalAppAccess, if (it.overrideConflicts == true) 1 else 0)
            }
        )
    }

    fun get(id: Long): List<Workspace> =
        db.query(NAME, where = "$ID = $id") { it.toWorkspace() }

    fun getAll(): List<Workspace> =
        db.query(NAME) { it.toWorkspace() }

    /** Updates just the conflict-resolution mode for [id], leaving the rest of the row (title,
     *  quests, etc.) untouched - called when workspace details are fetched, separately from the
     *  list sync in [put], since the workspace list endpoint doesn't return this field. */
    fun updateOverrideConflicts(id: Int, overrideConflicts: Boolean) {
        db.exec("UPDATE $NAME SET $OVERRIDE_CONFLICTS = ? WHERE $ID = ?", arrayOf(if (overrideConflicts) 1 else 0, id))
    }

    fun deleteAll(ids: List<Int>): Int {
        if (ids.isEmpty()) return 0
        return db.delete(NAME, "$ID in (${ids.joinToString(",")})")
    }

    private fun CursorPosition.toWorkspace() = Workspace(
        getInt(ID),
        getStringOrNull(QUESTS)?.split(",")?.map { number -> number.toInt() },
        getString(TITLE),
        getString(TYPE),
        getInt(EXTERNAL_APP_ACCESS),
        getInt(OVERRIDE_CONFLICTS) == 1,
    )
}
