package de.westnordost.streetcomplete.data.osm.edits.upload

import de.westnordost.streetcomplete.data.ConflictException
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditAction
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChanges
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryModify
import de.westnordost.streetcomplete.data.osm.edits.update_tags.UpdateElementTagsAction
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsManager
import de.westnordost.streetcomplete.data.osm.mapdata.ChangesetTooLargeException
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataApiClient
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataChanges
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataController
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataUpdates
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.testutils.any
import de.westnordost.streetcomplete.testutils.argumentCaptor
import de.westnordost.streetcomplete.testutils.capture
import de.westnordost.streetcomplete.testutils.edit
import de.westnordost.streetcomplete.testutils.eq
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.on
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ElementEditUploaderTest {

    private lateinit var changesetManager: OpenChangesetsManager
    private lateinit var mapDataApi: MapDataApiClient
    private lateinit var mapDataController: MapDataController
    private lateinit var uploader: ElementEditUploader
    private lateinit var pendingTagConflict: PendingTagConflictsController
    private lateinit var workspaceDao: WorkspaceDao

    @BeforeTest fun setUp() {
        changesetManager = mock()
        mapDataApi = mock()
        mapDataController = mock()
        pendingTagConflict = mock()
        workspaceDao = mock()
        uploader = ElementEditUploader(changesetManager, mapDataApi, mapDataController, pendingTagConflict, workspaceDao)
    }

    @Test fun `create new changeset when changeset is too large`(): Unit = runBlocking {
        val edit: ElementEdit = mock()
        val action: ElementEditAction = mock()
        on(edit.action).thenReturn(action)
        on(action.createUpdates(any(), any())).thenReturn(MapDataChanges())

        // current changeset is 1
        on(changesetManager.getOrCreateChangeset(any(), any(), any(), anyBoolean())).thenReturn(1L)
        // but when uploading using this changeset, exception is thrown
        on(mapDataApi.uploadChanges(eq(1L), any(), any())).thenThrow(ChangesetTooLargeException())

        // creating a changeset yields id 2
        on(changesetManager.createChangeset(any(), any(), any())).thenReturn(2)
        // and uploading changes to this changeset yields some result
        val mapDataUpdates = MapDataUpdates()
        on(mapDataApi.uploadChanges(eq(2L), any(), any())).thenReturn(mapDataUpdates)

        assertEquals(
            mapDataUpdates,
            uploader.upload(edit, { mock() })
        )
    }

    @Test fun `passes on conflict exception`(): Unit = runBlocking {
        val edit: ElementEdit = mock()
        val action: ElementEditAction = mock()
        on(edit.action).thenReturn(action)
        on(action.createUpdates(any(), any())).thenReturn(MapDataChanges())

        on(changesetManager.getOrCreateChangeset(any(), any(), any(), anyBoolean())).thenReturn(1)
        on(changesetManager.createChangeset(any(), any(), any())).thenReturn(1)
        on(mapDataApi.uploadChanges(anyLong(), any(), any())).thenThrow(ConflictException())

        assertFailsWith<ConflictException> {
            uploader.upload(edit, { mock() })
        }
    }

    @Test fun `handles changeset conflict exception`(): Unit = runBlocking {
        val edit: ElementEdit = mock()
        val action: ElementEditAction = mock()
        on(edit.action).thenReturn(action)
        on(action.createUpdates(any(), any())).thenReturn(MapDataChanges())

        on(changesetManager.getOrCreateChangeset(any(), any(), any(), anyBoolean())).thenReturn(1)
        on(changesetManager.createChangeset(any(), any(), any())).thenReturn(1)
        doThrow(ConflictException()).doAnswer { MapDataUpdates() }
            .on(mapDataApi).uploadChanges(anyLong(), any(), any())

        uploader.upload(edit, { mock() })
    }

    //region workspace conflict mode (Workspace.overrideConflicts, from the workspace details)

    /** The app changed surface asphalt -> concrete and added width, but meanwhile someone else
     *  changed surface to gravel on the server: a real conflict on "surface" only. */
    private fun conflictingEdit(workspaceId: Int = 7): ElementEdit {
        val original = node(id = 1, tags = mapOf("surface" to "asphalt"))
        val changes = StringMapChanges(listOf(
            StringMapEntryModify("surface", "asphalt", "concrete"),
            StringMapEntryAdd("width", "60"),
        ))
        return edit(action = UpdateElementTagsAction(original, changes)).also { it.workspaceId = workspaceId }
    }

    private suspend fun givenServerChangedSurface() {
        // not in the local cache -> the uploader goes to the server's current version
        on(mapDataController.get(any(), anyLong())).thenReturn(null)
        on(mapDataApi.getNode(1)).thenReturn(node(id = 1, tags = mapOf("surface" to "gravel"), version = 2))
        on(changesetManager.getOrCreateChangeset(any(), any(), any(), anyBoolean())).thenReturn(1L)
        on(mapDataApi.uploadChanges(anyLong(), any(), any())).thenReturn(MapDataUpdates())
    }

    private fun givenWorkspace(id: Int, overrideConflicts: Boolean) {
        on(workspaceDao.get(id.toLong())).thenReturn(listOf(
            Workspace(id = id, title = "Test Workspace", type = "osw", overrideConflicts = overrideConflicts)
        ))
    }

    @Test fun `OVERRIDE workspace uploads the app's value over a conflicting remote change`(): Unit = runBlocking {
        givenServerChangedSurface()
        givenWorkspace(7, overrideConflicts = true)

        uploader.upload(conflictingEdit(workspaceId = 7), { mock() })

        val uploaded = argumentCaptor<MapDataChanges>()
        verify(mapDataApi).uploadChanges(eq(1L), capture(uploaded), any())
        assertEquals(
            mapOf("surface" to "concrete", "width" to "60"),
            uploaded.value.modifications.single().tags
        )
        verify(pendingTagConflict, never()).add(any())
    }

    @Test fun `RESOLVE workspace holds the edit back for the user to decide`(): Unit = runBlocking {
        givenServerChangedSurface()
        givenWorkspace(7, overrideConflicts = false)

        assertFailsWith<HeldForConflictResolutionException> {
            uploader.upload(conflictingEdit(workspaceId = 7), { mock() })
        }

        val conflict = argumentCaptor<PendingTagConflict>()
        verify(pendingTagConflict).add(capture(conflict))
        assertEquals("surface", conflict.value.tagKey)
        assertEquals("concrete", conflict.value.mineValue)
        assertEquals("gravel", conflict.value.theirsValueAtDetection)
        assertEquals(7, conflict.value.workspaceId)
        verify(mapDataApi, never()).uploadChanges(anyLong(), any(), any())
    }

    @Test fun `unknown workspace falls back to RESOLVE`(): Unit = runBlocking {
        givenServerChangedSurface()
        on(workspaceDao.get(anyLong())).thenReturn(emptyList())

        assertFailsWith<HeldForConflictResolutionException> {
            uploader.upload(conflictingEdit(workspaceId = 7), { mock() })
        }
        verify(mapDataApi, never()).uploadChanges(anyLong(), any(), any())
    }

    @Test fun `the mode is looked up for the edit's own workspace`(): Unit = runBlocking {
        givenServerChangedSurface()
        givenWorkspace(7, overrideConflicts = false)
        givenWorkspace(8, overrideConflicts = true)

        uploader.upload(conflictingEdit(workspaceId = 8), { mock() })

        verify(workspaceDao).get(8L)
        verify(mapDataApi).uploadChanges(eq(1L), any(), any())
        verify(pendingTagConflict, never()).add(any())
    }

    @Test fun `without a real conflict the mode isn't needed`(): Unit = runBlocking {
        on(mapDataController.get(any(), anyLong())).thenReturn(null)
        // server still has the value the app started from
        on(mapDataApi.getNode(1)).thenReturn(node(id = 1, tags = mapOf("surface" to "asphalt"), version = 2))
        on(changesetManager.getOrCreateChangeset(any(), any(), any(), anyBoolean())).thenReturn(1L)
        on(mapDataApi.uploadChanges(anyLong(), any(), any())).thenReturn(MapDataUpdates())

        uploader.upload(conflictingEdit(workspaceId = 7), { mock() })

        verify(workspaceDao, never()).get(anyLong())
        verify(mapDataApi).uploadChanges(eq(1L), any(), any())
    }

    //endregion
}

