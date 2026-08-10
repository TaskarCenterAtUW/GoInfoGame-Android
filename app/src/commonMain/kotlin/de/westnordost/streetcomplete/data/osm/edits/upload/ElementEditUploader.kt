package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.ApplicationConstants.EDIT_ACTIONS_NOT_ALLOWED_TO_USE_LOCAL_CHANGES
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementIdProvider
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.changesApplied
import de.westnordost.streetcomplete.data.osm.edits.update_tags.isGeometrySubstantiallyDifferent
import de.westnordost.streetcomplete.data.osm.edits.update_tags.mineValue
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
     *  @throws HeldForConflictResolutionException if some of the edit's tag changes collided with
     *          a concurrent remote edit - nothing was uploaded, the edit should be blocked until
     *          the user has resolved the pending conflicts
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
     * - if any key genuinely collides, the *whole edit* is held back: nothing is uploaded,
     *   a [PendingTagConflict] is recorded per colliding key, and
     *   [HeldForConflictResolutionException] is thrown so the caller blocks the edit until the
     *   user has decided per tag. The decisions are folded into the edit and it then uploads
     *   normally, as one unit - so the edit history always shows exactly what was pushed.
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

        val realConflicts = action.changes.getConflictsTo(currentElement.tags).toSet()

        if (realConflicts.isNotEmpty()) {
            for (conflict in realConflicts) {
                pendingTagConflictsController.add(
                    PendingTagConflict(
                        id = 0,
                        editId = edit.id,
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
            throw HeldForConflictResolutionException(currentElement)
        }

        val changes = MapDataChanges(
            modifications = listOf(currentElement.changesApplied(action.changes))
        )
        return try {
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
}

/** Thrown when an edit's tag changes collided with a concurrent remote edit: nothing was
 *  uploaded, [PendingTagConflict]s were recorded, and the edit should be blocked from uploading
 *  until the user has resolved them. Carries the freshly fetched [currentElement] so the caller
 *  can refresh the local cache to the state the conflicts were detected against. */
class HeldForConflictResolutionException(val currentElement: Element) :
    RuntimeException("Edit held back until its tag conflicts are resolved")
