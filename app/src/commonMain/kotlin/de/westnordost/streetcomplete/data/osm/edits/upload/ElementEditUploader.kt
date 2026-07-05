package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.ApplicationConstants.EDIT_ACTIONS_NOT_ALLOWED_TO_USE_LOCAL_CHANGES
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementIdProvider
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChanges
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryChange
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryDelete
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryModify
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.changesApplied
import de.westnordost.streetcomplete.data.osm.edits.update_tags.isGeometrySubstantiallyDifferent
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.ChangesetTooLargeException
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiClient
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataChanges
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataUpdates
import de.westnordost.streetcomplete.data.osm.mapdata.RemoteMapDataRepository
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds

class ElementEditUploader(
    private val changesetManager: OpenChangesetsManager,
    private val mapDataApi: MapDataApiClient,
    private val mapDataController: MapDataController,
    private val pendingTagConflictsController: PendingTagConflictsController,
) {

    /** Apply the given change to the given element and upload it
     *
     *  @throws ConflictException if element has been changed server-side in an incompatible way
     */
    suspend fun upload(edit: ElementEdit, getIdProvider: () -> ElementIdProvider): MapDataUpdates {
        // certain edit types don't allow building changes on top of cached map data
        val mustUseRemoteData = edit.action::class in EDIT_ACTIONS_NOT_ALLOWED_TO_USE_LOCAL_CHANGES

        return if (mustUseRemoteData) {
            uploadUsingRemoteRepo(edit, getIdProvider)
        } else {
            // we first try to apply the changes onto the element cached locally, then upload...
            try {
                val localChanges = edit.action.createUpdates(mapDataController, getIdProvider())
                try {
                    uploadChanges(edit, localChanges, false)
                }
                // changeset already too large -> try again with new changeset
                catch (e: ChangesetTooLargeException) {
                    uploadChanges(edit, localChanges, true)
                }
            }
            // ...but this can fail for various reasons:
            // - the changeset is already closed on remote
            // - the element was modified on remote in the meantime
            // - there's a conflict when applying the change to the locally cached element
            // - the element does not exist in the local database (cache was deleted)
            //
            // In any case -> try again with remote data
            catch (e: ConflictException) {
                uploadUsingRemoteRepo(edit, getIdProvider)
            }
        }
    }

    /**
     *  Apply the given edit to data downloaded ad-hoc from remote, then upload it.
     *
     *  @throws ConflictException if element has been changed on remote in an incompatible way
     * */
    private suspend fun uploadUsingRemoteRepo(edit: ElementEdit, getIdProvider: () -> ElementIdProvider): MapDataUpdates {
        val action = edit.action
        // tag-only edits get special handling: a colliding key doesn't have to sink the whole
        // edit, see uploadTagChangesUsingRemoteRepo
        if (action is UpdateElementTagsAction) {
            return uploadTagChangesUsingRemoteRepo(edit, action)
        }

        // If a conflict is thrown here, it definitely means that the element has been changed on
        // remote in an incompatible way. So, we don't catch the exception but exit
        val remoteChanges = action.createUpdates(RemoteMapDataRepository(mapDataApi), getIdProvider())

        return try {
            uploadChanges(edit, remoteChanges, false)
        }
        // probably changeset was closed -> try again once with new changeset
        catch (e: ConflictException) {
            uploadChanges(edit, remoteChanges, true)
        }
        // changeset too large -> also try again once with new changeset
        catch (e: ChangesetTooLargeException) {
            uploadChanges(edit, remoteChanges, true)
        }
    }

    /**
     * Applies a tag-update edit onto the element's current (freshly fetched) remote state, but
     * unlike other edit types, a per-key value collision does not discard the whole edit:
     * - keys that don't collide with the current remote tags are merged and uploaded right away
     * - the two bookkeeping keys are always force-overwritten with the local value, never treated
     *   as a conflict
     * - any other genuinely colliding key is held back as a [PendingTagConflict] for the user to
     *   resolve later, instead of losing the whole batch of answers
     *
     * Structural issues (element deleted, geometry changed substantially) are still a hard,
     * all-or-nothing failure - there's no sensible per-tag override for those.
     */
    private suspend fun uploadTagChangesUsingRemoteRepo(edit: ElementEdit, action: UpdateElementTagsAction): MapDataUpdates {
        val original = action.originalElement
        val currentElement = fetchElement(original.type, original.id)
            ?: throw ConflictException("Element deleted")

        if (isGeometrySubstantiallyDifferent(original, currentElement)) {
            throw ConflictException("Element geometry changed substantially")
        }

        // bookkeeping tags are never shown to the user as a conflict - rebuild them against the
        // element's current value so they never register as colliding and always win with the
        // local (i.e. most recent) value
        val reconciledChanges = action.changes.changes.map { change ->
            if (change.key in SILENTLY_OVERRIDDEN_TAG_KEYS) {
                change.rebuiltAgainst(currentElement.tags[change.key])
            } else {
                change
            }
        }.toSet()

        val realConflicts = StringMapChanges(reconciledChanges).getConflictsTo(currentElement.tags).toSet()
        val safeChanges = StringMapChanges(reconciledChanges - realConflicts)

        val updates = if (safeChanges.isEmpty()) {
            // nothing could be merged - just refresh the local cache to the current server state
            MapDataUpdates(updated = listOf(currentElement))
        } else {
            val updatedElement = currentElement.changesApplied(safeChanges)
            val changes = MapDataChanges(modifications = listOf(updatedElement))
            try {
                uploadChanges(edit, changes, false)
            }
            // probably changeset was closed -> try again once with new changeset
            catch (e: ConflictException) {
                uploadChanges(edit, changes, true)
            }
            catch (e: ChangesetTooLargeException) {
                uploadChanges(edit, changes, true)
            }
        }

        for (conflict in realConflicts) {
            pendingTagConflictsController.add(
                PendingTagConflict(
                    id = 0,
                    elementType = currentElement.type,
                    elementId = currentElement.id,
                    tagKey = conflict.key,
                    mineValue = conflict.mineValue(),
                    theirsValueAtDetection = currentElement.tags[conflict.key],
                    editType = edit.type,
                    source = edit.source,
                    position = edit.position,
                    createdTimestamp = nowAsEpochMilliseconds(),
                    workspaceId = edit.workspaceId
                )
            )
        }

        return updates
    }

    private suspend fun fetchElement(type: ElementType, id: Long): Element? = when (type) {
        ElementType.NODE -> mapDataApi.getNode(id)
        ElementType.WAY -> mapDataApi.getWay(id)
        ElementType.RELATION -> mapDataApi.getRelation(id)
    }

    private suspend fun uploadChanges(
        edit: ElementEdit,
        changes: MapDataChanges,
        newChangeset: Boolean
    ): MapDataUpdates {
        val changesetId = if (newChangeset) {
            changesetManager.createChangeset(edit.type, edit.source, edit.position)
        } else {
            changesetManager.getOrCreateChangeset(edit.type, edit.source, edit.position, edit.isNearUserLocation)
        }
        return mapDataApi.uploadChanges(changesetId, changes, ApplicationConstants::ignoreRelation)
    }

    companion object {
        /** tags that are set on every long-form answer purely for bookkeeping/quest-visibility
         *  purposes - never worth bothering the user with a conflict prompt about, see
         *  EditDescription.kt which excludes the same two keys from edit-history display */
        private val SILENTLY_OVERRIDDEN_TAG_KEYS = setOf("ext:gig_complete", "ext:gig_last_updated")
    }
}

private fun StringMapEntryChange.mineValue(): String? = when (this) {
    is StringMapEntryAdd -> value
    is StringMapEntryModify -> value
    is StringMapEntryDelete -> null
}

private fun StringMapEntryChange.rebuiltAgainst(currentValue: String?): StringMapEntryChange {
    val mine = mineValue()
    return when {
        mine != null && currentValue != null -> StringMapEntryModify(key, currentValue, mine)
        mine != null && currentValue == null -> StringMapEntryAdd(key, mine)
        mine == null && currentValue != null -> StringMapEntryDelete(key, currentValue)
        else -> this
    }
}
