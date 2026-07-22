package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.EDIT_ID
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.PHOTO_PATHS
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import kotlinx.serialization.json.Json

/** Stores the local file paths of photos attached to a not-yet-synced create-feature edit,
 *  one row per edit. The paths are uploaded (and the row deleted) at sync time. */
class FeaturePhotosDao(
    private val db: Database,
    private val preferences: Preferences? = null,
) {
    private val json = Json

    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(editId: Long, paths: List<String>) {
        db.insert(NAME, listOf(
            EDIT_ID to editId,
            PHOTO_PATHS to json.encodeToString(paths),
            WORKSPACE_ID to workspaceId
        ))
    }

    fun get(editId: Long): List<String> =
        db.queryOne(
            NAME,
            where = "$WORKSPACE_ID = $workspaceId AND $EDIT_ID = $editId"
        ) { json.decodeFromString<List<String>>(it.getString(PHOTO_PATHS)) } ?: emptyList()

    fun delete(editId: Long): Boolean =
        db.delete(NAME, "$WORKSPACE_ID = $workspaceId AND $EDIT_ID = $editId") > 0
}
