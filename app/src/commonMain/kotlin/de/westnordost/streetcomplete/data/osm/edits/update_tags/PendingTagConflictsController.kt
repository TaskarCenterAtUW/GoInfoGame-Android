package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiClient
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataChanges
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.util.Listeners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Holds tag-level edit conflicts that were held back instead of being discarded (see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditUploader]), and lets the user
 *  resolve them one at a time - either by re-asserting their own answer or by accepting the
 *  concurrent remote edit's value. */
class PendingTagConflictsController(
    private val dao: PendingTagConflictsDao,
    private val mapDataApi: MapDataApiClient,
    private val mapDataController: MapDataController,
    private val openChangesetsManager: OpenChangesetsManager,
) {
    interface Listener {
        fun onAdded(conflict: PendingTagConflict)
        fun onRemoved(conflict: PendingTagConflict)
    }

    private val listeners = Listeners<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun add(conflict: PendingTagConflict) {
        val added = dao.add(conflict)
        listeners.forEach { it.onAdded(added) }
    }

    fun getAll(): List<PendingTagConflict> = dao.getAll()

    /** Number of elements with at least one pending conflict, not the number of conflicting tags -
     *  matches the grouped-by-element dialog (see TagConflictResolutionEffect), where several
     *  conflicting tags on the same element are shown and resolved together as one */
    fun getCount(): Int = dao.getAll().distinctBy { it.elementType to it.elementId }.size

    /** Returns the oldest pending conflict without removing it - it stays queryable/re-showable
     *  until actually resolved, so it survives the app being killed mid-decision */
    fun getOldest(): PendingTagConflict? = dao.getAll().firstOrNull()

    /** All pending conflicts for the same element as the oldest one, so they can be shown - and
     *  decided on - together in a single dialog instead of one at a time */
    fun getOldestGroup(): List<PendingTagConflict> {
        val all = dao.getAll()
        val oldest = all.firstOrNull() ?: return emptyList()
        return all.filter { it.elementType == oldest.elementType && it.elementId == oldest.elementId }
    }

    /** Re-assert the user's own answer for this tag, re-fetching the element fresh first so we
     *  don't race against a third edit that may have landed since the conflict was detected.
     *  Returns null if resolved (applied, or turned out to be a no-op), or an updated
     *  [PendingTagConflict] (with a fresh "theirs" value) if the value changed *again* in the
     *  meantime and the user should be asked again with up-to-date information. */
    suspend fun resolveKeepMine(conflict: PendingTagConflict): PendingTagConflict? = withContext(
        Dispatchers.IO
    ) {
        val currentElement = fetchElement(conflict.elementType, conflict.elementId)
        if (currentElement == null) {
            remove(conflict)
            return@withContext null
        }
        val currentValue = currentElement.tags[conflict.tagKey]
        val change = buildChange(conflict.tagKey, conflict.mineValue, currentValue)
        if (change == null) {
            // already matches what the user wants (or both sides agree it should be absent)
            remove(conflict)
            return@withContext null
        }
        val updatedElement = currentElement.changesApplied(StringMapChanges(setOf(change)))
        val changes = MapDataChanges(modifications = listOf(updatedElement))

        try {
            val updates = uploadWithChangesetRetry(conflict, changes)
            mapDataController.updateAll(updates)
            remove(conflict)
            null
        } catch (e: ConflictException) {
            // someone changed this tag yet again while we were resolving - re-fetch and let the
            // user decide again with current data rather than silently failing
            val refetched = fetchElement(conflict.elementType, conflict.elementId)
            if (refetched == null) {
                remove(conflict)
                null
            } else {
                remove(conflict)
                val refreshed =
                    dao.add(conflict.copy(theirsValueAtDetection = refetched.tags[conflict.tagKey]))
                listeners.forEach { it.onAdded(refreshed) }
                refreshed
            }
        }
    }

    /** Accept the concurrent remote edit's value - no network call needed, the server already
     *  has the value being kept */
    suspend fun resolveKeepTheirs(conflict: PendingTagConflict) = withContext(Dispatchers.IO) {
        remove(conflict)
    }

    private suspend fun uploadWithChangesetRetry(
        conflict: PendingTagConflict,
        changes: MapDataChanges,
    ) =
        try {
            val changesetId = openChangesetsManager.getOrCreateChangeset(
                conflict.editType, conflict.source, conflict.position, false
            )
            mapDataApi.uploadChanges(changesetId, changes)
        } catch (e: ConflictException) {
            // could be a stale changeset that's been closed in the meantime - try once more with
            // a fresh one before giving up and treating it as a real data conflict
            val newChangesetId = openChangesetsManager.createChangeset(
                conflict.editType,
                conflict.source,
                conflict.position
            )
            mapDataApi.uploadChanges(newChangesetId, changes)
        }

    private suspend fun fetchElement(type: ElementType, id: Long): Element? = when (type) {
        ElementType.NODE -> mapDataApi.getNode(id)
        ElementType.WAY -> mapDataApi.getWay(id)
        ElementType.RELATION -> mapDataApi.getRelation(id)
    }

    private fun remove(conflict: PendingTagConflict) {
        if (dao.delete(conflict.id)) listeners.forEach { it.onRemoved(conflict) }
    }

    private fun buildChange(
        key: String,
        mineValue: String?,
        currentValue: String?,
    ): StringMapEntryChange? = when {
        mineValue != null && currentValue != null -> StringMapEntryModify(
            key,
            currentValue,
            mineValue
        )

        mineValue != null && currentValue == null -> StringMapEntryAdd(key, mineValue)
        mineValue == null && currentValue != null -> StringMapEntryDelete(key, currentValue)
        else -> null
    }
}
