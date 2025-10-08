package de.westnordost.streetcomplete.data.workspace.data.repository

import android.location.Location
import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkspaceRepositoryImpl(
    private val apiService: WorkspaceApiService,
    private val dao: WorkspaceDao,
) : WorkspaceRepository {

    override fun getWorkspaces(location: Location): Flow<List<Workspace>> = flow {
        // emit(dao.getAll())
        val workspaces = apiService.getWorkspaces(location)
        dao.getAll().map { it.id }.let { ids ->
            dao.deleteAll(ids)
        }
        dao.put(workspaces)
        emit(workspaces)
    }

    override fun getWorkspaceDetails(workspaceId: Int): Flow<WorkspaceDetailsResponse> {
        return flow {
            val longForms = apiService.getWorkspaceDetails(workspaceId)
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

}
