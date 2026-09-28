package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.streetcomplete.data.AuthorizationException
import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.karta_view.KartaViewApiClient
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditAction
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.data.osm.edits.create_feature.StuckPhotoUploadNoticesController
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiClient
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataUpdates
import de.westnordost.streetcomplete.data.osmnotes.edits.NoteEditsController
import de.westnordost.streetcomplete.data.upload.OnUploadedChangeListener
import de.westnordost.streetcomplete.data.user.statistics.StatisticsController
import de.westnordost.streetcomplete.testutils.any
import de.westnordost.streetcomplete.testutils.edit
import de.westnordost.streetcomplete.testutils.eq
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.on
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ElementEditsUploaderTest {

    private lateinit var elementEditsController: ElementEditsController
    private lateinit var mapDataController: MapDataController
    private lateinit var noteEditsController: NoteEditsController
    private lateinit var singleUploader: ElementEditUploader
    private lateinit var mapDataApi: MapDataApiClient
    private lateinit var statisticsController: StatisticsController
    private lateinit var discardedEditNoticesController: DiscardedEditNoticesController
    private lateinit var imageUploader: KartaViewApiClient
    private lateinit var featurePhotosController: FeaturePhotosController
    private lateinit var stuckPhotoUploadNoticesController: StuckPhotoUploadNoticesController
    private lateinit var changesetManager: OpenChangesetsManager

    private lateinit var uploader: ElementEditsUploader
    private lateinit var listener: OnUploadedChangeListener

    @BeforeTest fun setUp() {
        elementEditsController = mock()
        mapDataController = mock()
        noteEditsController = mock()

        singleUploader = mock()
        mapDataApi = mock()
        statisticsController = mock()
        discardedEditNoticesController = mock()
        imageUploader = mock()
        featurePhotosController = mock()
        stuckPhotoUploadNoticesController = mock()
        changesetManager = mock()

        listener = mock()

        uploader = ElementEditsUploader(
            elementEditsController,
            noteEditsController,
            mapDataController,
            singleUploader,
            mapDataApi,
            statisticsController,
            discardedEditNoticesController,
            imageUploader,
            featurePhotosController,
            stuckPhotoUploadNoticesController,
            changesetManager,
        )
        uploader.uploadedChangeListener = listener
    }

    @Test fun `cancel upload works`() = runBlocking {
        val job = launch { uploader.upload() }
        job.cancelAndJoin()
        verifyNoInteractions(elementEditsController, mapDataController, singleUploader, statisticsController)
    }

    @Test fun `upload works`() = runBlocking {
        val edit = edit()
        val updates = mock<MapDataUpdates>()

        on(elementEditsController.getOldestUnsynced()).thenReturn(edit).thenReturn(null)
        on(singleUploader.upload(any(), any())).thenReturn(updates)

        uploader.upload()

        verify(singleUploader).upload(eq(edit), any())
        verify(listener).onUploaded(any(), any())
        verify(elementEditsController).markSynced(edit, updates)
        verify(noteEditsController).updateElementIds(any())
        verify(mapDataController).updateAll(updates)

        verify(statisticsController).addOne(any(), any())
    }

    @Test fun `upload catches conflict exception`() = runBlocking {
        // edit modifies node 1 and way 1
        val node1 = node()
        val action: ElementEditAction = mock()
        on(action.elementKeys).thenReturn(listOf(
            ElementKey(ElementType.NODE, 1),
            ElementKey(ElementType.WAY, 1),
        ))
        val edit = edit(action = action)

        // ...but way 1 is gone
        on(mapDataApi.getNode(1)).thenReturn(node1)
        on(mapDataApi.getWayComplete(1)).thenReturn(null)

        // the edit is the first in the upload queue and the uploader throws a conflict exception
        on(elementEditsController.getOldestUnsynced()).thenReturn(edit).thenReturn(null)
        on(singleUploader.upload(any(), any())).thenThrow(ConflictException())

        uploader.upload()

        verify(singleUploader).upload(eq(edit), any())
        verify(listener).onDiscarded(any(), any())

        verify(elementEditsController).markSyncFailed(edit)
        verifyNoInteractions(statisticsController)

        verify(mapDataController).updateAll(eq(MapDataUpdates(
            updated = listOf(node1),
            deleted = listOf(ElementKey(ElementType.WAY, 1))
        )))
    }

    //region conflict handling and changesets

    // RESOLVE mode: ElementEditUploader held the edit because a tag collided with a remote edit
    @Test fun `an edit held for conflict resolution is blocked, not synced and not discarded`() = runBlocking {
        val edit = edit()
        val serverVersion = node(id = 1, tags = mapOf("surface" to "gravel"), version = 2)
        on(elementEditsController.getOldestUnsynced()).thenReturn(edit).thenReturn(null)
        on(singleUploader.upload(any(), any())).thenThrow(HeldForConflictResolutionException(serverVersion))

        uploader.upload()

        verify(elementEditsController).markBlockedOnConflict(edit)
        // the local map now shows what the conflict was detected against
        verify(mapDataController).updateAll(eq(MapDataUpdates(updated = listOf(serverVersion))))
        verify(elementEditsController, never()).markSynced(any(), any())
        verify(elementEditsController, never()).markSyncFailed(any())
        verifyNoInteractions(discardedEditNoticesController, statisticsController, listener)
    }

    @Test fun `a held edit doesn't stop the edits queued behind it`() = runBlocking {
        val held = edit(id = 1)
        val next = edit(id = 2)
        val updates = MapDataUpdates()
        on(elementEditsController.getOldestUnsynced()).thenReturn(held).thenReturn(next).thenReturn(null)
        on(singleUploader.upload(eq(held), any())).thenThrow(HeldForConflictResolutionException(node()))
        on(singleUploader.upload(eq(next), any())).thenReturn(updates)

        uploader.upload()

        verify(elementEditsController).markBlockedOnConflict(held)
        verify(elementEditsController).markSynced(next, updates)
    }

    @Test fun `open changesets are closed after every upload run`() = runBlocking {
        on(elementEditsController.getOldestUnsynced()).thenReturn(edit()).thenReturn(null)
        on(singleUploader.upload(any(), any())).thenReturn(MapDataUpdates())

        uploader.upload()

        verify(changesetManager).closeAllOpenChangesets()
    }

    @Test fun `open changesets are closed even when the upload run fails`() = runBlocking {
        on(elementEditsController.getOldestUnsynced()).thenReturn(edit()).thenReturn(null)
        on(singleUploader.upload(any(), any())).thenThrow(AuthorizationException())

        assertFailsWith<AuthorizationException> { uploader.upload() }

        verify(changesetManager).closeAllOpenChangesets()
    }

    //endregion
}

