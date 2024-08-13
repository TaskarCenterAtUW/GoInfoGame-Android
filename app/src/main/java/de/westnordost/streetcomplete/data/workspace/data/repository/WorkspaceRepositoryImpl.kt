package de.westnordost.streetcomplete.data.workspace.data.repository

import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkspaceRepositoryImpl(
    private val apiService: WorkspaceApiService,
    private val dao: WorkspaceDao,
) : WorkspaceRepository {

    override fun getWorkspaces(): Flow<List<Workspace>> = flow {
        emit(dao.getAll())
        val workspaces = apiService.getWorkspaces()
        dao.put(workspaces)
        emit(workspaces)
    }

    override fun getLongFormForWorkspace(workspaceId : Int): Flow<List<AddLongFormResponseItem>> {
        return flow {
            val longForms = apiService.getLongFormForWorkspace(workspaceId)
            emit(longForms)
        }
    }

    override fun loginToWorkspace(username: String, password: String): Flow<LoginResponse> = flow {
        val loginResponse = apiService.loginToWorkspace(username, password)
        emit(loginResponse)
    }
}
