package de.westnordost.streetcomplete.data.osm.edits.create_feature

import de.westnordost.streetcomplete.util.Listeners

/** Holds notices that an edit's photo(s) repeatedly failed to upload to KartaView (see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditsUploader]) - unlike
 *  [de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesController], the user CAN
 *  act on these: keep trying, or drop the photo so the rest of the edit can still be submitted. */
class StuckPhotoUploadNoticesController(
    private val dao: StuckPhotoUploadNoticesDao,
) {
    interface Listener {
        fun onAdded(notice: StuckPhotoUploadNotice)
        fun onRemoved(notice: StuckPhotoUploadNotice)
    }
    private val listeners = Listeners<Listener>()

    fun addListener(listener: Listener) { listeners.add(listener) }
    fun removeListener(listener: Listener) { listeners.remove(listener) }

    fun add(notice: StuckPhotoUploadNotice) {
        val added = dao.add(notice)
        listeners.forEach { it.onAdded(added) }
    }

    fun getCount(): Int = dao.getCount()

    /** Returns the oldest notice without removing it - it stays queryable/re-showable until
     *  actually dismissed, so it survives the app being killed before the user sees it */
    fun getOldest(): StuckPhotoUploadNotice? = dao.getAll().firstOrNull()

    fun dismiss(notice: StuckPhotoUploadNotice) {
        if (dao.delete(notice.id)) listeners.forEach { it.onRemoved(notice) }
    }
}
