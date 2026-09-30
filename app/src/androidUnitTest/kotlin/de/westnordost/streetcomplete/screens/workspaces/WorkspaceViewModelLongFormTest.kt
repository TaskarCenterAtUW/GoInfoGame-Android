package de.westnordost.streetcomplete.screens.workspaces

import android.location.Location
import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.UserLoginController
import de.westnordost.streetcomplete.data.workspace.LEGACY_LONG_FORM
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.VALID_LONG_FORM
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.assertValidLongForm
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.workspaceDetailsJson
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import de.westnordost.streetcomplete.testutils.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.mockito.Mockito.verify
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tapping a workspace in the list: [WorkspaceViewModelImpl.getWorkspaceDetails] turns the
 * workspace details into the long form the map is then opened with, or into an error the list
 * shows in a snackbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelLongFormTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferences = Preferences(MapSettings())
    private val repository = FakeDetailsRepository()
    private val downloadedTilesController = mock<DownloadedTilesController>()
    private lateinit var viewModel: WorkspaceViewModelImpl

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WorkspaceViewModelImpl(
            repository,
            preferences,
            downloadedTilesController,
            UserLoginController(preferences, EnvironmentManager(preferences)),
            File("does-not-exist.json"),
        )
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun serve(longFormQuestDef: String, overrideConflicts: String? = null) {
        repository.details = {
            flowOf(json.decodeFromString<WorkspaceDetailsResponse>(
                workspaceDetailsJson(id = it, overrideConflicts = overrideConflicts, longFormQuestDef = longFormQuestDef)
            ))
        }
    }

    /** The state getWorkspaceDetails settles on after Loading. */
    private suspend fun openWorkspace(id: Int = 7): WorkspaceLongFormState {
        var result: WorkspaceLongFormState? = null
        viewModel.getWorkspaceDetails(id).test {
            assertEquals(WorkspaceLongFormState.Loading, awaitItem())
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result!!
    }

    //region valid long form

    @Test fun `tapping a workspace fetches that workspace's details`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM)
        openWorkspace(7)
        assertEquals(listOf(7), repository.detailsRequests)
    }

    @Test fun `a versioned long form opens with all its parts`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM)
        val state = assertIs<WorkspaceLongFormState.Success>(openWorkspace())

        assertValidLongForm(state.longFormItems)
        val sidewalks = state.longFormItems.single()
        assertEquals("Sidewalks", sidewalks.elementType)
        assertEquals("ways with (highway=footway and footway=sidewalk)", sidewalks.questQuery)
        assertEquals(
            listOf("ExclusiveChoice", "TextEntry", "Numeric", "MultipleChoice"),
            sidewalks.quests.map { it?.questType }
        )
        assertEquals(30, state.recencyPeriodInDays)
        assertEquals(listOf("Bench"), state.featurePresets.map { it.name })
        assertEquals(mapOf("amenity" to "bench"), state.featurePresets.single().tags)
        assertEquals(listOf("streetlight"), state.customIcons.map { it.name })
        assertNull(state.imageryList)
    }

    @Test fun `questions arrive unanswered and visible`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM)
        val state = assertIs<WorkspaceLongFormState.Success>(openWorkspace())
        for (quest in state.longFormItems.single().quests.filterNotNull()) {
            assertNull(quest.userInput, "${quest.questId}")
            assertNull(quest.selectedIndex, "${quest.questId}")
            assertTrue(quest.visible, "${quest.questId}")
        }
    }

    @Test fun `recency period defaults to 90 days when the long form has none`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM.replace(""""recency_period": 30,""", ""))
        val state = assertIs<WorkspaceLongFormState.Success>(openWorkspace())
        assertEquals(90, state.recencyPeriodInDays)
    }

    @Test fun `a legacy long form (bare elements array) still opens`() = runTest(testDispatcher) {
        serve(LEGACY_LONG_FORM)
        val state = assertIs<WorkspaceLongFormState.Success>(openWorkspace())

        assertValidLongForm(state.longFormItems)
        assertEquals("Kerb", state.longFormItems.single().elementType)
        assertEquals(90, state.recencyPeriodInDays)
        assertTrue(state.featurePresets.isEmpty())
        assertTrue(state.customIcons.isEmpty())
    }

    @Test fun `the conflict mode doesn't affect the long form`() = runTest(testDispatcher) {
        for (mode in listOf("true", "false", null)) {
            serve(VALID_LONG_FORM, overrideConflicts = mode)
            assertIs<WorkspaceLongFormState.Success>(openWorkspace(), "overrideConflicts=$mode")
        }
    }

    @Test fun `opening a workspace clears the selection, so it isn't opened again`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM)
        viewModel.setSelectedWorkspace(Workspace(id = 7, title = "Test Workspace", type = "osw"))
        openWorkspace(7)
        assertNull(viewModel.selectedWorkspace.value)
    }

    @Test fun `selecting a workspace remembers it and invalidates downloaded tiles`() {
        viewModel.setSelectedWorkspace(Workspace(id = 7, title = "Test Workspace", type = "osw"))
        assertEquals(7, preferences.workspaceId)
        assertEquals(7, viewModel.selectedWorkspace.value?.id)
        verify(downloadedTilesController).invalidateAll()
    }

    //endregion

    //region invalid long form

    @Test fun `no long form at all is an error`() = runTest(testDispatcher) {
        serve("null")
        assertEquals(
            WorkspaceLongFormState.Error("Unexpected JSON structure for long form (Null or invalid).  Please contact the admin for this workspace"),
            openWorkspace()
        )
    }

    @Test fun `a long form without elements is an error`() = runTest(testDispatcher) {
        val expected = WorkspaceLongFormState.Error("No long form quests available for this workspace")

        serve("""{"version":"3.2.0","elements":[]}""")
        assertEquals(expected, openWorkspace())

        serve("[]")
        assertEquals(expected, openWorkspace())
    }

    @Test fun `a quest query that doesn't parse is an error`() = runTest(testDispatcher) {
        serve(VALID_LONG_FORM.replace("ways with (highway=footway and footway=sidewalk)", "ways with (highway="))
        val error = assertIs<WorkspaceLongFormState.Error>(openWorkspace())
        assertTrue(
            error.error!!.startsWith("Workspace is not configured properly. Please contact the admin for this workspace"),
            error.error
        )
    }

    @Test fun `a long form with the wrong shape is an error, not a crash`() = runTest(testDispatcher) {
        serve("""{"version":"3.2.0","elements":[{"element_type":"Sidewalks","quests":"not a list"}]}""")
        val error = assertIs<WorkspaceLongFormState.Error>(openWorkspace())
        assertTrue(error.error!!.startsWith("Workspace is not configured properly"), error.error)
    }

    @Test fun `failing to fetch the details is an error with the message, and clears the selection`() = runTest(testDispatcher) {
        repository.details = { flow { throw Exception("Failed. Workspace not found with ID : 7") } }
        viewModel.setSelectedWorkspace(Workspace(id = 7, title = "Test Workspace", type = "osw"))

        assertEquals(WorkspaceLongFormState.Error("Failed. Workspace not found with ID : 7"), openWorkspace())
        assertNull(viewModel.selectedWorkspace.value)
    }

    //endregion
}

private class FakeDetailsRepository : WorkspaceRepository {
    var details: (Int) -> Flow<WorkspaceDetailsResponse> = { emptyFlow() }
    val detailsRequests = mutableListOf<Int>()

    override fun getWorkspaceDetails(workspaceId: Int): Flow<WorkspaceDetailsResponse> {
        detailsRequests.add(workspaceId)
        return details(workspaceId)
    }

    override fun getWorkspaces(location: Location): Flow<List<Workspace>> = emptyFlow()
    override fun getUserProjectGroups(): Flow<List<UserProjectGroupItem>> = emptyFlow()
    override fun loginToWorkspace(username: String, password: String): Flow<LoginResponse> = emptyFlow()
    override fun getUserInfo(userEmail: String): Flow<UserInfoResponse> = emptyFlow()
    override fun refreshToken(refreshToken: String): Flow<LoginResponse> = emptyFlow()
    override fun getAppUpdateInfo(): Flow<AppUpdateCheckerResponse> = emptyFlow()
    override fun clearCachedAuthTokens() {}
}
