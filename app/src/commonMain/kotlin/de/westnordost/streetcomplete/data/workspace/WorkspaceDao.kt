package de.westnordost.streetcomplete.data.workspace

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.EXTERNAL_APP_ACCESS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.ID
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.QUESTS
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.TITLE
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.Columns.TYPE
import de.westnordost.streetcomplete.data.workspace.WorkSpaceTable.NAME

class WorkspaceDao(private val db: Database) {

    fun put(responseItems: List<Workspace>) {
        db.replaceMany(
            NAME,
            arrayOf(ID, TITLE, QUESTS, TYPE, EXTERNAL_APP_ACCESS),
            responseItems.map { arrayOf(it.id, it.title, it.quests?.joinToString(","),it.type, it.externalAppAccess) }
        )
    }

    fun get(id: Long): List<Workspace> =
        db.query(NAME, where = "$ID = $id") {
            Workspace(
                it.getInt(ID),
                it.getStringOrNull(QUESTS)?.split(",")?.map { number -> number.toInt() },
                it.getString(TITLE),it.getString(TYPE), it.getInt(EXTERNAL_APP_ACCESS))
        }

    fun getAll(): List<Workspace> =
        db.query(NAME) {
            Workspace(
                it.getInt(ID),
                it.getStringOrNull(QUESTS)?.split(",")?.map { number -> number.toInt() },
                it.getString(TITLE),it.getString(TYPE), it.getInt(EXTERNAL_APP_ACCESS))
        }

    fun deleteAll(ids: List<Int>): Int {
        if (ids.isEmpty()) return 0
        return db.delete(NAME, "$ID in (${ids.joinToString(",")})")
    }
}
