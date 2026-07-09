package de.westnordost.streetcomplete.data.osm.edits.update_tags

import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import de.westnordost.streetcomplete.util.Listeners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Holds tag-level conflicts of edits that are blocked from uploading (see
 *  [de.westnordost.streetcomplete.data.osm.edits.upload.ElementEditUploader]): while any of these
 *  exist for an edit, nothing of that edit is uploaded. The user resolves them per tag - keep
 *  their own answer or accept the concurrent remote edit's value - and each decision is folded
 *  into the blocked edit's stored action. Once the last conflict of an edit is resolved, the edit
 *  is unblocked and uploads through the normal sync path as one unit, so the edit history shows
 *  exactly what was pushed. */
class PendingTagConflictsController(
    private val dao: PendingTagConflictsDao,
    private val elementEditsController: ElementEditsController,
) {
    interface Listener {
        fun onAdded(conflict: PendingTagConflict)
        fun onRemoved(conflict: PendingTagConflict)
    }

    private val listeners = Listeners<Listener>()

    init {
        // an edit deleted while blocked (e.g. undone from the edit history) takes its pending
        // conflicts with it
        elementEditsController.addListener(object : ElementEditsSource.Listener {
            override fun onAddedEdit(edit: ElementEdit) {}
            override fun onSyncedEdit(edit: ElementEdit) {}
            override fun onDeletedEdits(edits: List<ElementEdit>) {
                val editIds = edits.mapTo(HashSet()) { it.id }
                for (conflict in dao.getAll().filter { it.editId in editIds }) {
                    remove(conflict)
                }
            }
        })
    }

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

    /** Number of blocked edits with at least one pending conflict, not the number of conflicting
     *  tags - matches the grouped dialog (see TagConflictResolutionEffect), where all conflicting
     *  tags of the same edit are shown and resolved together as one */
    fun getCount(): Int = dao.getAll().distinctBy { it.editId }.size

    /** Returns the oldest pending conflict without removing it - it stays queryable/re-showable
     *  until actually resolved, so it survives the app being killed mid-decision */
    fun getOldest(): PendingTagConflict? = dao.getAll().firstOrNull()

    /** All pending conflicts of the same (blocked) edit as the oldest one, so they can be shown -
     *  and decided on - together in a single dialog instead of one at a time */
    fun getOldestGroup(): List<PendingTagConflict> {
        val all = dao.getAll()
        val oldest = all.firstOrNull() ?: return emptyList()
        return all.filter { it.editId == oldest.editId }
    }

    /** Re-assert the user's own answer for this tag: rebuild the kept change in the blocked
     *  edit's action against the value the conflict was detected against, so the re-upload's
     *  conflict check doesn't re-flag it for the very collision the user just decided on. (If a
     *  *third* value lands before the re-upload, upload-time detection catches it and the edit
     *  is held again with fresh data.) */
    suspend fun resolveKeepMine(conflict: PendingTagConflict) = withContext(Dispatchers.IO) {
        updateBlockedEdit(conflict) { change -> change.rebuiltAgainst(conflict.theirsValueAtDetection) }
        remove(conflict)
        unblockIfFullyResolved(conflict.editId)
    }

    /** Accept the concurrent remote edit's value: drop this tag's change from the blocked edit's
     *  action - the server already has the value being kept */
    suspend fun resolveKeepTheirs(conflict: PendingTagConflict) = withContext(Dispatchers.IO) {
        updateBlockedEdit(conflict) { null }
        remove(conflict)
        unblockIfFullyResolved(conflict.editId)
    }

    /** Rewrite the conflict's tag change within its blocked edit's action; a null result from
     *  [rewrite] drops the change */
    private fun updateBlockedEdit(
        conflict: PendingTagConflict,
        rewrite: (StringMapEntryChange) -> StringMapEntryChange?,
    ) {
        val edit = elementEditsController.get(conflict.editId) ?: return
        val action = edit.action as? UpdateElementTagsAction ?: return
        val newChanges = action.changes.changes.mapNotNull { change ->
            if (change.key == conflict.tagKey) rewrite(change) else change
        }.toSet()
        if (newChanges != action.changes.changes) {
            elementEditsController.updateAction(edit.copy(action = action.copy(changes = StringMapChanges(newChanges))))
        }
    }

    private fun unblockIfFullyResolved(editId: Long) {
        if (dao.getAll().any { it.editId == editId }) return
        val edit = elementEditsController.get(editId) ?: return
        if (edit.isBlockedOnConflict) elementEditsController.markUnblocked(edit)
    }

    private fun remove(conflict: PendingTagConflict) {
        if (dao.delete(conflict.id)) listeners.forEach { it.onRemoved(conflict) }
    }
}

internal fun StringMapEntryChange.mineValue(): String? = when (this) {
    is StringMapEntryAdd -> value
    is StringMapEntryModify -> value
    is StringMapEntryDelete -> null
}

/** The same intended end value (or deletion), but expressed as a diff from [currentValue] instead
 *  of from whatever this change's original baseline was */
internal fun StringMapEntryChange.rebuiltAgainst(currentValue: String?): StringMapEntryChange {
    val mine = mineValue()
    return when {
        mine != null && currentValue != null -> StringMapEntryModify(key, currentValue, mine)
        mine != null && currentValue == null -> StringMapEntryAdd(key, mine)
        mine == null && currentValue != null -> StringMapEntryDelete(key, currentValue)
        else -> this
    }
}
