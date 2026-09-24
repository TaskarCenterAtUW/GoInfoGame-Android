package de.westnordost.streetcomplete.screens.workspaces

import android.location.Location
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory stand-in for [WorkspaceRepository] used by [WorkspaceLoginFlowTest] so the login ->
 * workspace-list UI flow can be exercised without a real backend. Registered into Koin in place
 * of [de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl].
 *
 * Every auth call succeeds by default; tests swap in failures via [login], [userInfo] and
 * [refresh] (e.g. `userInfo = { flow { throw Exception("User profile not found.") } }`).
 */
class FakeWorkspaceRepository : WorkspaceRepository {

    var login: (username: String) -> Flow<LoginResponse> = { flowOf(TEST_LOGIN_RESPONSE) }
    var userInfo: (email: String) -> Flow<UserInfoResponse> = { email ->
        flowOf(
            UserInfoResponse(
                id = TEST_USER_ID,
                email = email,
                firstName = "Test",
                lastName = "User",
                username = email,
            )
        )
    }
    var refresh: (refreshToken: String) -> Flow<LoginResponse> = { flowOf(TEST_LOGIN_RESPONSE) }

    override fun getWorkspaces(location: Location): Flow<List<Workspace>> = flowOf(
        listOf(
            Workspace(
                id = 1,
                title = TEST_WORKSPACE_TITLE,
                type = "osw",
                externalAppAccess = 1,
                createdAt = "2025-01-01T00:00:00Z",
            )
        )
    )

    override fun getUserProjectGroups(): Flow<List<UserProjectGroupItem>> = flowOf(emptyList())

    override fun getWorkspaceDetails(workspaceId: Int): Flow<WorkspaceDetailsResponse> = emptyFlow()

    override fun loginToWorkspace(username: String, password: String): Flow<LoginResponse> = login(username)

    override fun getUserInfo(userEmail: String): Flow<UserInfoResponse> = userInfo(userEmail)

    override fun refreshToken(refreshToken: String): Flow<LoginResponse> = refresh(refreshToken)

    // no-op - this in-memory fake has no real HttpClient/BearerAuthProvider to clear
    override fun clearCachedAuthTokens() {}

    // no environment's version numbers ever compare higher than the installed build, so the
    // force/optional update dialogs in AppForceUpdateHandler never appear and block the test
    override fun getAppUpdateInfo(): Flow<AppUpdateCheckerResponse> = flowOf(
        AppUpdateCheckerResponse(
            android = AppUpdateCheckerResponse.Android(
                dev = AppUpdateCheckerResponse.Android.AppVersions("0.0.0", "0.0.0"),
                prod = AppUpdateCheckerResponse.Android.AppVersions("0.0.0", "0.0.0"),
                stage = AppUpdateCheckerResponse.Android.AppVersions("0.0.0", "0.0.0"),
            )
        )
    )

    companion object {
        const val TEST_WORKSPACE_TITLE = "Test Workspace"
        const val TEST_USER_ID = "fake-user-id"
        val TEST_LOGIN_RESPONSE = LoginResponse(
            access_token = "fake-access-token",
            expires_in = 3600,
            refresh_expires_in = 7200,
            refresh_token = "fake-refresh-token",
        )
    }
}
