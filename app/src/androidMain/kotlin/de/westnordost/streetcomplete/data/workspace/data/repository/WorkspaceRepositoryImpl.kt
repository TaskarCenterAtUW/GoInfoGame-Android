package de.westnordost.streetcomplete.data.workspace.data.repository

import android.location.Location
import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkspaceRepositoryImpl(
    private val apiService: WorkspaceApiService,
    private val dao: WorkspaceDao,
) : WorkspaceRepository {

    override fun getWorkspaces(location: Location): Flow<List<Workspace>> = flow {
        // emit(dao.getAll())
        // the list endpoint doesn't return overrideConflicts (only the per-workspace details
        // endpoint does, see getWorkspaceDetails below) - preserve whatever was already persisted
        // for each workspace across this delete-all-and-replace sync, so it isn't reset to the
        // RESOLVE default every time the workspace list is refreshed
        val existingOverrideConflicts = dao.getAll().associate { it.id to it.overrideConflicts }
        val workspaces = apiService.getWorkspaces(location)
        dao.getAll().map { it.id }.let { ids ->
            dao.deleteAll(ids)
        }
        dao.put(workspaces.map { it.copy(overrideConflicts = existingOverrideConflicts[it.id] ?: it.overrideConflicts) })
        emit(workspaces)
    }

    override fun getUserProjectGroups(): Flow<List<UserProjectGroupItem>> = flow {
        emit(apiService.getUserProjectGroups())
    }

    override fun getWorkspaceDetails(workspaceId: Int): Flow<WorkspaceDetailsResponse> {
        return flow {
            val longForms = apiService.getWorkspaceDetails(workspaceId)
            // null/missing overrideConflicts means RESOLVE mode (the default)
            dao.updateOverrideConflicts(workspaceId, longForms.overrideConflicts ?: false)
            emit(longForms)
        }
    }

    override fun loginToWorkspace(username: String, password: String): Flow<LoginResponse> = flow {
        val loginResponse = apiService.loginToWorkspace(username, password)
        emit(loginResponse)
    }

    override fun getUserInfo(userEmail: String): Flow<UserInfoResponse> = flow {
        val userInfoResponse = apiService.getTDEIUserDetails(userEmail)
        emit(userInfoResponse)
    }

    override fun refreshToken(refreshToken: String): Flow<LoginResponse> =
        flow { emit(apiService.refreshToken(refreshToken)) }

    override fun getAppUpdateInfo(): Flow<AppUpdateCheckerResponse> =
        flow { emit(apiService.getForceUpdateInfo()) }

    override fun clearCachedAuthTokens() {
        apiService.clearCachedAuthTokens()
    }

}
