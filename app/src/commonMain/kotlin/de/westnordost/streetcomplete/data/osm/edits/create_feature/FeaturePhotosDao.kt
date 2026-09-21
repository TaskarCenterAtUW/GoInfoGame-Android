package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.EDIT_ID
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.PHOTO_BEARINGS
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.PHOTO_PATHS
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.Columns.WORKSPACE_ID
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable.NAME
import de.westnordost.streetcomplete.data.preferences.Preferences
import kotlinx.serialization.json.Json

/** Stores the local file paths (and capture bearings) of photos attached to a not-yet-synced
 *  create-feature/tag-update edit, one row per edit. Uploaded (and the row deleted) at sync time. */
class FeaturePhotosDao(
    private val db: Database,
    private val preferences: Preferences? = null,
) {
    private val json = Json

    private val workspaceId
        get() = preferences?.workspaceId ?: 0

    fun add(editId: Long, photos: List<FeaturePhoto>) {
        db.insert(NAME, listOf(
            EDIT_ID to editId,
            PHOTO_PATHS to json.encodeToString(photos.map { it.path }),
            PHOTO_BEARINGS to json.encodeToString(photos.map { it.bearing }),
            WORKSPACE_ID to workspaceId
        ))
    }

    fun get(editId: Long): List<FeaturePhoto> =
        db.queryOne(
            NAME,
            where = "$WORKSPACE_ID = $workspaceId AND $EDIT_ID = $editId"
        ) { row ->
            val paths = json.decodeFromString<List<String>>(row.getString(PHOTO_PATHS))
            // absent on rows written before PHOTO_BEARINGS existed - treated as all-zero bearings
            val bearings = row.getStringOrNull(PHOTO_BEARINGS)?.let { json.decodeFromString<List<Float>>(it) }
            paths.mapIndexed { i, path -> FeaturePhoto(path, bearings?.getOrNull(i) ?: 0f) }
        } ?: emptyList()

    fun delete(editId: Long): Boolean =
        db.delete(NAME, "$WORKSPACE_ID = $workspaceId AND $EDIT_ID = $editId") > 0
}
