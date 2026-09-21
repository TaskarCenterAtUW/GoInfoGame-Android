package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/** One photo attached to a not-yet-synced edit, with the compass bearing (0-359, clockwise from
 *  north) the device was facing at capture time - or 0f when a caller doesn't track it (not every
 *  photo-attaching flow captures a bearing yet). Uploaded with that bearing at sync time - see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader.uploadPendingPhotos]. */
@Serializable
data class FeaturePhoto(val path: String, val bearing: Float = 0f)

/** Manages the photos attached to not-yet-synced create-feature/tag-update edits: their file
 *  paths (and capture bearings) are held here until the edit is uploaded, at which point the
 *  photos are uploaded first (see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader]) and then cleaned
 *  up via [markUploaded]. */
class FeaturePhotosController(
    private val dao: FeaturePhotosDao,
    private val elementEditsSource: ElementEditsSource,
    private val fileSystem: FileSystem,
) {
    init {
        // an edit deleted before it synced (e.g. undone from the edit history) takes its photos
        // with it
        elementEditsSource.addListener(object : ElementEditsSource.Listener {
            override fun onAddedEdit(edit: ElementEdit) {}
            override fun onSyncedEdit(edit: ElementEdit) {}
            override fun onDeletedEdits(edits: List<ElementEdit>) {
                for (edit in edits) deletePhotos(edit.id)
            }
        })
    }

    fun add(editId: Long, photos: List<FeaturePhoto>) = dao.add(editId, photos)

    fun get(editId: Long): List<FeaturePhoto> = dao.get(editId)

    fun markUploaded(editId: Long) = deletePhotos(editId)

    private fun deletePhotos(editId: Long) {
        for (photo in dao.get(editId)) {
            fileSystem.delete(Path(photo.path), mustExist = false)
        }
        dao.delete(editId)
    }
}
