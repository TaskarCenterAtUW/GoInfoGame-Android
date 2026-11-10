package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import de.westnordost.streetcomplete.data.photos.CapturedPhoto
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/** One photo attached to a not-yet-synced edit, with the compass bearing (0-359, clockwise from
 *  true north) the camera was facing at capture time - or null if unknown (not every
 *  photo-attaching flow captures a bearing yet) - and the long-form [question] it was taken for,
 *  if any, and when it was taken ([capturedAt], epoch milliseconds - null if not tracked, the
 *  edit's creation time is used then). Uploaded with those at sync time - see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader.uploadPendingPhotos]. */
@Serializable
data class FeaturePhoto(
    val path: String,
    val bearing: Float? = null,
    val question: String? = null,
    val questId: Int? = null,
    val capturedAt: Long? = null,
) {
    constructor(photo: CapturedPhoto, question: String? = null, questId: Int? = null) :
        this(photo.path, photo.bearing, question, questId, photo.capturedAt)

    val captured: CapturedPhoto get() = CapturedPhoto(path, bearing, capturedAt)
}

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

    /** Record another failed upload attempt for this edit's photo(s), returning the new streak
     *  count - see [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader] */
    fun incrementUploadAttempts(editId: Long): Int = dao.incrementUploadAttempts(editId)

    /** Reset the failed-upload streak, e.g. once the user chooses to keep trying */
    fun resetUploadAttempts(editId: Long) = dao.resetUploadAttempts(editId)

    private fun deletePhotos(editId: Long) {
        for (photo in dao.get(editId)) {
            fileSystem.delete(Path(photo.path), mustExist = false)
        }
        dao.delete(editId)
    }
}
