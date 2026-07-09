package de.westnordost.streetcomplete.data.osm.edits

import de.westnordost.streetcomplete.util.Listeners

/** Holds notices about edits that had to be discarded because the underlying element was deleted
 *  or changed too substantially in the meantime (see ElementEditsUploader) - purely informational,
 *  there is nothing to resolve, unlike [de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController].
 *  Lets the user find out an answer was lost instead of the quest just silently reappearing later. */
class DiscardedEditNoticesController(
    private val dao: DiscardedEditNoticesDao,
) {
    interface Listener {
        fun onAdded(notice: DiscardedEditNotice)
        fun onRemoved(notice: DiscardedEditNotice)
    }
    private val listeners = Listeners<Listener>()

    fun addListener(listener: Listener) { listeners.add(listener) }
    fun removeListener(listener: Listener) { listeners.remove(listener) }

    fun add(notice: DiscardedEditNotice) {
        val added = dao.add(notice)
        listeners.forEach { it.onAdded(added) }
    }

    fun getAll(): List<DiscardedEditNotice> = dao.getAll()

    fun getCount(): Int = dao.getCount()

    /** Returns the oldest notice without removing it - it stays queryable/re-showable until
     *  actually dismissed, so it survives the app being killed before the user sees it */
    fun getOldest(): DiscardedEditNotice? = dao.getAll().firstOrNull()

    fun dismiss(notice: DiscardedEditNotice) {
        if (dao.delete(notice.id)) listeners.forEach { it.onRemoved(notice) }
    }
}
