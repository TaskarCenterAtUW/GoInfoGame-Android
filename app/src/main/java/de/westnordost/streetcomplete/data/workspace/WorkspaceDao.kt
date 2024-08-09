package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.ID
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.QUESTS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.TITLE
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.NAME
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace

class WorkspaceDao(private val db: Database) {

    fun put(responseItems: List<Workspace>) {
        db.replaceMany(
            NAME,
            arrayOf(ID, TITLE, QUESTS),
            responseItems.map { arrayOf(it.id, it.title, it.quests?.joinToString(",")) }
        )
    }

    fun get(id: Long): List<Workspace> =
        db.query(NAME, where = "$ID = $id") {
            Workspace(
                it.getInt(ID),
                it.getString(QUESTS).split(",").map { number -> number.toInt() },
                it.getString(TITLE))
        }

    fun getAll(): List<Workspace> =
        db.query(NAME) {
            Workspace(
                it.getInt(ID),
                it.getString(QUESTS).split(",").map { number -> number.toInt() },
                it.getString(TITLE))
        }

    fun deleteAll(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        return db.delete(NAME, "$ID in (${ids.joinToString(",")})")
    }
}
