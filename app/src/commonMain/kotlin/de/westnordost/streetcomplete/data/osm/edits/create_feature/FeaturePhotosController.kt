package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path

/** Manages the photos attached to not-yet-synced create-feature edits: their file paths are held
 *  here until the edit is uploaded, at which point the photos are uploaded first (see
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

    fun add(editId: Long, paths: List<String>) = dao.add(editId, paths)

    fun get(editId: Long): List<String> = dao.get(editId)

    fun markUploaded(editId: Long) = deletePhotos(editId)

    private fun deletePhotos(editId: Long) {
        for (path in dao.get(editId)) {
            fileSystem.delete(Path(path), mustExist = false)
        }
        dao.delete(editId)
    }
}
