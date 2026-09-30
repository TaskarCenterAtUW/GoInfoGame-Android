package de.westnordost.streetcomplete.screens.workspaces

import android.location.Location
import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.UserLoginController
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceAuthRejectedException
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import de.westnordost.streetcomplete.testutils.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelAuthTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferences = Preferences(MapSettings())
    private val repository = FakeAuthRepository()
    private lateinit var viewModel: WorkspaceViewModelImpl

    private val login = LoginResponse(
        access_token = "access", expires_in = 300, refresh_expires_in = 1800, refresh_token = "refresh"
    )
    private val userInfo = UserInfoResponse(id = "user-1", username = "jdoe", firstName = "Jane", lastName = "Doe")

    @BeforeTest fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WorkspaceViewModelImpl(
            repository,
            preferences,
            mock<DownloadedTilesController>(),
            UserLoginController(preferences, EnvironmentManager(preferences)),
            File("does-not-exist.json"),
        )
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    //region login

    @Test fun `successful login goes loading then success`() = runTest(testDispatcher) {
        repository.login = { flow { emit(login) } }
        viewModel.loginState.test {
            assertEquals(WorkspaceLoginState.Init, awaitItem())
            viewModel.loginToWorkspace("user@example.com", "pw")
            assertEquals(WorkspaceLoginState.Loading, awaitItem())
            assertEquals(WorkspaceLoginState.Success(login, "user@example.com"), awaitItem())
        }
    }

    @Test fun `failed login goes loading then error with the message`() = runTest(testDispatcher) {
        repository.login = { flow { throw Exception("Invalid username or password.") } }
        viewModel.loginState.test {
            skipItems(1)
            viewModel.loginToWorkspace("u", "p")
            assertEquals(WorkspaceLoginState.Loading, awaitItem())
            assertEquals(WorkspaceLoginState.Error("Invalid username or password."), awaitItem())
        }
        assertFalse(preferences.workspaceLogin)
    }

    //endregion

    //region setLoginState

    @Test fun `setLoginState stores the session and the user`() = runTest(testDispatcher) {
        repository.userInfo = { flow { emit(userInfo) } }
        val before = System.currentTimeMillis()
        viewModel.setLoginState(true, login, "user@example.com")

        assertTrue(preferences.workspaceLogin)
        assertEquals("access", preferences.workspaceToken)
        assertEquals("refresh", preferences.workspaceRefreshToken)
        assertEquals("user@example.com", preferences.workspaceUserEmail)
        assertEquals(300_000L, preferences.accessTokenExpiryInterval)
        assertEquals(1_800_000L, preferences.refreshTokenExpiryInterval)
        assertTrue(preferences.workspaceLastLogin >= before)
        assertEquals(preferences.workspaceLastLogin + 300_000L, preferences.accessTokenExpiryTime)
        assertEquals(preferences.workspaceLastLogin + 1_800_000L, preferences.refreshTokenExpiryTime)
        assertEquals("user-1", preferences.workspaceUserId)
        assertEquals("jdoe \n Jane Doe", preferences.workspaceUserName)
        assertEquals(listOf("user@example.com"), repository.userInfoRequests)
    }

    @Test fun `setLoginState rolls the session back when the user profile can't be fetched`() = runTest(testDispatcher) {
        // regression: login succeeded, the profile call 404'd, and the app was left with
        // workspaceLogin = true but no user id - no error shown, and the next start skipped login
        repository.userInfo = { flow { throw Exception("User profile not found.") } }

        val e = assertFailsWith<Exception> { viewModel.setLoginState(true, login, "user@example.com") }
        assertEquals("User profile not found.", e.message)

        assertFalse(preferences.workspaceLogin)
        assertNull(preferences.workspaceToken)
        assertNull(preferences.workspaceRefreshToken)
        assertNull(preferences.workspaceUserId)
        assertEquals(1, repository.clearCachedAuthTokensCalls)
        assertEquals(WorkspaceLoginState.Error("User profile not found."), viewModel.loginState.value)
    }

    @Test fun `a retry after a failed profile fetch logs in normally`() = runTest(testDispatcher) {
        repository.userInfo = { flow { throw Exception("User profile not found.") } }
        assertFailsWith<Exception> { viewModel.setLoginState(true, login, "user@example.com") }

        repository.userInfo = { flow { emit(userInfo) } }
        viewModel.setLoginState(true, login, "user@example.com")
        assertTrue(preferences.workspaceLogin)
        assertEquals("user-1", preferences.workspaceUserId)
    }

    //endregion

    //region refreshToken

    @Test fun `refresh stores the rotated tokens and reports success for the stored user`() = runTest(testDispatcher) {
        preferences.workspaceRefreshToken = "old-refresh"
        preferences.workspaceUserEmail = "user@example.com"
        repository.refresh = { flow { emit(login) } }

        viewModel.refreshToken()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("old-refresh"), repository.refreshRequests)
        assertEquals(WorkspaceLoginState.Success(login, "user@example.com", expediteLogin = false), viewModel.loginState.value)
        assertTrue(preferences.workspaceLogin)
        assertEquals("access", preferences.workspaceToken)
        assertEquals("refresh", preferences.workspaceRefreshToken)
        assertEquals(preferences.workspaceLastLogin + 1_800_000L, preferences.refreshTokenExpiryTime)
        assertEquals(preferences.workspaceLastLogin + 300_000L, preferences.accessTokenExpiryTime)
    }

    @Test fun `refresh rejected by the server is an error`() = runTest(testDispatcher) {
        preferences.workspaceRefreshToken = "old-refresh"
        repository.refresh = { flow { throw WorkspaceAuthRejectedException("Your session has expired. Please log in again.") } }

        viewModel.refreshToken()
        testScheduler.advanceUntilIdle()

        assertEquals(WorkspaceLoginState.Error("Your session has expired. Please log in again."), viewModel.loginState.value)
    }

    @Test fun `refresh failing for any other reason is a network error, which must not log out`() = runTest(testDispatcher) {
        preferences.workspaceRefreshToken = "old-refresh"
        for (failure in listOf(IOException("offline"), Exception("The server is temporarily unavailable. Please try again later."))) {
            repository.refresh = { flow { throw failure } }
            viewModel.refreshToken()
            testScheduler.advanceUntilIdle()
            assertIs<WorkspaceLoginState.NetworkError>(viewModel.loginState.value, "for $failure")
        }
        assertEquals("old-refresh", preferences.workspaceRefreshToken)
    }

    @Test fun `refresh without a stored refresh token is an error without calling the server`() = runTest(testDispatcher) {
        viewModel.refreshToken()
        testScheduler.advanceUntilIdle()
        assertEquals(WorkspaceLoginState.Error("No refresh token found"), viewModel.loginState.value)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test fun `refresh without a stored user email is an error`() = runTest(testDispatcher) {
        preferences.workspaceRefreshToken = "old-refresh"
        repository.refresh = { flow { emit(login) } }
        viewModel.refreshToken()
        testScheduler.advanceUntilIdle()
        assertEquals(WorkspaceLoginState.Error("No user email found"), viewModel.loginState.value)
    }

    //endregion

    @Test fun `switching environment clears the session and the cached tokens`() {
        preferences.workspaceLogin = true
        preferences.workspaceToken = "access"
        preferences.workspaceRefreshToken = "refresh"

        viewModel.resetSessionForEnvironmentChange()

        assertFalse(preferences.workspaceLogin)
        assertNull(preferences.workspaceToken)
        assertNull(preferences.workspaceRefreshToken)
        assertEquals(1, repository.clearCachedAuthTokensCalls)
    }
}

private class FakeAuthRepository : WorkspaceRepository {
    var login: (String) -> Flow<LoginResponse> = { emptyFlow() }
    var userInfo: (String) -> Flow<UserInfoResponse> = { emptyFlow() }
    var refresh: (String) -> Flow<LoginResponse> = { emptyFlow() }

    val userInfoRequests = mutableListOf<String>()
    val refreshRequests = mutableListOf<String>()
    var clearCachedAuthTokensCalls = 0

    override fun loginToWorkspace(username: String, password: String) = login(username)
    override fun getUserInfo(userEmail: String): Flow<UserInfoResponse> {
        userInfoRequests.add(userEmail)
        return userInfo(userEmail)
    }
    override fun refreshToken(refreshToken: String): Flow<LoginResponse> {
        refreshRequests.add(refreshToken)
        return refresh(refreshToken)
    }
    override fun clearCachedAuthTokens() { clearCachedAuthTokensCalls++ }

    override fun getWorkspaces(location: Location): Flow<List<Workspace>> = emptyFlow()
    override fun getUserProjectGroups(): Flow<List<UserProjectGroupItem>> = emptyFlow()
    override fun getWorkspaceDetails(workspaceId: Int): Flow<WorkspaceDetailsResponse> = emptyFlow()
    override fun getAppUpdateInfo(): Flow<AppUpdateCheckerResponse> = emptyFlow()
}
