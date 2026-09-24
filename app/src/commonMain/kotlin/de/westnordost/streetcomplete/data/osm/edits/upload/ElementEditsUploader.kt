package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.karta_view.KartaViewApiClient
import de.westnordost.streetcomplete.data.karta_view.KartaViewException
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNotice
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesController
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementIdProvider
import de.westnordost.streetcomplete.data.osm.edits.IsRevertAction
import de.westnordost.streetcomplete.data.osm.edits.create.CreateNodeAction
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.data.osm.edits.create_feature.StuckPhotoUploadNotice
import de.westnordost.streetcomplete.data.osm.edits.create_feature.StuckPhotoUploadNoticesController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.withTag
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.MapData
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiClient
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataUpdates
import de.westnordost.streetcomplete.data.osm.mapdata.MutableMapData
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsController
import de.westnordost.streetcomplete.data.upload.OnUploadedChangeListener
import de.westnordost.streetcomplete.data.user.statistics.StatisticsController
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.logs.Log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ElementEditsUploader(
    private val elementEditsController: ElementEditsController,
    private val noteEditsController: NoteEditsController,
    private val mapDataController: MapDataController,
    private val singleUploader: ElementEditUploader,
    private val mapDataApi: MapDataApiClient,
    private val statisticsController: StatisticsController,
    private val discardedEditNoticesController: DiscardedEditNoticesController,
    private val imageUploader: KartaViewApiClient,
    private val featurePhotosController: FeaturePhotosController,
    private val stuckPhotoUploadNoticesController: StuckPhotoUploadNoticesController,
    private val changesetManager: OpenChangesetsManager,
) {
    var uploadedChangeListener: OnUploadedChangeListener? = null

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + CoroutineName("ElementEditsUploader"))

    suspend fun upload() = mutex.withLock { withContext(Dispatchers.IO) {
        // KartaView is a third-party service outside our control, and can be down/flaky
        // independently of everything else - a photo upload failure here must never block every
        // OTHER queued edit (e.g. plain tag-only quests with no photo at all) from uploading.
        // Tracked only in memory, for this one upload() run: nothing is written to the DB for a
        // failed edit, so it's a completely normal unsynced edit again afterwards, and the very
        // next upload run (auto-triggered often - see QuestAutoSyncer) retries it fresh.
        val failedEditIds = mutableSetOf<Long>()
        try {
            while (true) {
                val edit = elementEditsController.getOldestUnsynced(failedEditIds) ?: break
                val getIdProvider: () -> ElementIdProvider = { elementEditsController.getIdProvider(edit.id) }
                try {
                    /* the sync of local change -> API and its response should not be cancellable
                     * because otherwise an inconsistency in the data would occur. E.g. no "star" for
                     * an uploaded change, a change could be uploaded twice etc */
                    withContext(scope.coroutineContext) { uploadEdit(edit, getIdProvider) }
                } catch (e: KartaViewException) {
                    /* the edit's photos failed to upload (plain network failure) - leave the edit
                     * unsynced for the next sync attempt instead of letting this bubble up and abort
                     * uploading of any other edits still queued, e.g. note edits (see
                     * uploadPendingPhotos KDoc). Skip past it for the REST of this run too (instead
                     * of retrying the same failing edit in a tight loop, or breaking out and
                     * abandoning every other queued edit behind it - see the failedEditIds KDoc). */
                    Log.w(TAG, "Failed to upload photos for edit ${edit.id}, will retry on next sync: ${e.message}")
                    failedEditIds += edit.id
                    onPhotoUploadFailed(edit)
                }
            }
        } finally {
            // close immediately after every upload run (instead of waiting for the 20-minute
            // inactivity auto-closer) so each batch of edits lands in its own closed changeset,
            // for traceability - in a finally so whatever was uploaded before an exception above
            // still gets its changeset closed
            changesetManager.closeAllOpenChangesets()
        }
    } }

    /** Bumps the edit's photo-upload-failure streak and, once it reaches [STUCK_PHOTO_UPLOAD_ATTEMPTS]
     *  (KartaView failing this many times in a row - not just a one-off network blip), raises a
     *  notice so the user can choose to keep waiting or drop the photo and submit without it.
     *  Only fires once per streak: as long as the notice is still unresolved, further failures
     *  keep incrementing past the threshold without adding duplicate notices for the same edit. */
    private fun onPhotoUploadFailed(edit: ElementEdit) {
        val attempts = featurePhotosController.incrementUploadAttempts(edit.id)
        if (attempts != STUCK_PHOTO_UPLOAD_ATTEMPTS) return
        val elementKey = edit.action.elementKeys.firstOrNull()
        stuckPhotoUploadNoticesController.add(
            StuckPhotoUploadNotice(
                id = 0,
                editId = edit.id,
                editType = edit.type,
                elementType = elementKey?.type,
                elementId = elementKey?.id,
                position = edit.position,
                createdTimestamp = nowAsEpochMilliseconds(),
                workspaceId = edit.workspaceId
            )
        )
    }

    private suspend fun uploadEdit(edit: ElementEdit, getIdProvider: () -> ElementIdProvider) {
        /* photos attached to a create-feature edit are uploaded first, OUTSIDE the conflict
           try/catch: a photo upload failure is a plain network failure (the edit stays unsynced
           and is retried on the next sync), never a reason to discard the edit */
        @Suppress("NAME_SHADOWING")
        val edit = uploadPendingPhotos(edit)
        val editActionClassName = edit.action::class.simpleName!!

        try {
            val updates = singleUploader.upload(edit, getIdProvider)

            Log.d(TAG, "Uploaded a $editActionClassName")
            uploadedChangeListener?.onUploaded(edit.type.name, edit.position)

            elementEditsController.markSynced(edit, updates)
            mapDataController.updateAll(updates)
            noteEditsController.updateElementIds(updates.idUpdates)

            if (edit.action is IsRevertAction) {
                statisticsController.subtractOne(edit.type.name, edit.position)
            } else {
                statisticsController.addOne(edit.type.name, edit.position)
            }
        } catch (e: HeldForConflictResolutionException) {
            // nothing was uploaded and nothing is discarded: the edit is excluded from uploading
            // until the user has resolved its pending tag conflicts, then re-enters this queue
            Log.d(TAG, "Held a $editActionClassName for conflict resolution")
            elementEditsController.markBlockedOnConflict(edit)
            // refresh the local cache to the remote state the conflicts were detected against
            mapDataController.updateAll(MapDataUpdates(updated = listOf(e.currentElement)))
        } catch (e: ConflictException) {
            Log.d(TAG, "Dropped a $editActionClassName: ${e.message}")
            uploadedChangeListener?.onDiscarded(edit.type.name, edit.position)

            elementEditsController.markSyncFailed(edit)
            val elementKey = edit.action.elementKeys.firstOrNull()
            discardedEditNoticesController.add(
                DiscardedEditNotice(
                    id = 0,
                    editType = edit.type,
                    elementType = elementKey?.type,
                    elementId = elementKey?.id,
                    position = edit.position,
                    reason = e.message ?: "Could not be applied to the current state of the map",
                    createdTimestamp = nowAsEpochMilliseconds(),
                    workspaceId = edit.workspaceId
                )
            )

            /* fetching the current version of the element(s) edited on conflict and persisting
               them is not really optional, as when the edit has been deleted due to the conflict,
               the quests etc. would otherwise just be displayed again as if the user didn't solve
               them */
            val updated = mutableListOf<Element>()
            val deleted = mutableListOf<ElementKey>()

            for (elementKey in edit.action.elementKeys) {
                val mapData = fetchElementComplete(elementKey.type, elementKey.id)
                if (mapData != null) {
                    updated.addAll(mapData)
                } else {
                    deleted.add(elementKey)
                }
            }
            if (updated.isNotEmpty() || deleted.isNotEmpty()) {
                mapDataController.updateAll(MapDataUpdates(updated = updated, deleted = deleted))
            }
        }
    }

    /** If the edit is a node creation or a long-form tag update with photo(s) still awaiting
     *  upload, uploads them to KartaView and folds the resulting URL(s) into the action's tags -
     *  ext:image_url1, ext:image_url2, ... (one per image, in attach order) for a node creation,
     *  or the single ext:kartaview_url for a tag update (long form only ever attaches one photo
     *  per edit - see ALongForm). The rewritten action is persisted BEFORE the photo records/files
     *  are deleted, so a crash in between cannot lose the URL(s) or upload the photos twice.
     *  Returns the edit whose action carries the URL tag(s). */
    private suspend fun uploadPendingPhotos(edit: ElementEdit): ElementEdit {
        val action = edit.action
        if (action !is CreateNodeAction && action !is UpdateElementTagsAction) return edit
        val photos = featurePhotosController.get(edit.id)
        if (photos.isEmpty()) return edit

        val urls = imageUploader.upload(photos.map { it.path to it.bearing }, edit.position)
        var uploadedEdit = edit
        if (urls.isNotEmpty()) {
            uploadedEdit = when (action) {
                is CreateNodeAction -> {
                    val urlTags = urls.mapIndexed { i, url -> "ext:image_url${i + 1}" to url }
                    edit.copy(action = action.copy(tags = action.tags + urlTags))
                }
                is UpdateElementTagsAction -> {
                    val changes = action.changes.withTag(
                        KARTAVIEW_URL_TAG, urls.first(), action.originalElement.tags
                    )
                    edit.copy(action = action.copy(changes = changes))
                }
                else -> edit
            }
            elementEditsController.updateAction(uploadedEdit)
        }
        featurePhotosController.markUploaded(edit.id)
        return uploadedEdit
    }

    private suspend fun fetchElementComplete(elementType: ElementType, elementId: Long): MapData? =
        when (elementType) {
            ElementType.NODE -> mapDataApi.getNode(elementId)?.let { MutableMapData(listOf(it)) }
            ElementType.WAY -> mapDataApi.getWayComplete(elementId)
            ElementType.RELATION -> mapDataApi.getRelationComplete(elementId)
        }

    companion object {
        private const val TAG = "ElementEditsUploader"
        private const val KARTAVIEW_URL_TAG = "ext:kartaview_url"
        /** consecutive KartaView upload failures for the same edit before nagging the user about it */
        private const val STUCK_PHOTO_UPLOAD_ATTEMPTS = 3
    }
}
