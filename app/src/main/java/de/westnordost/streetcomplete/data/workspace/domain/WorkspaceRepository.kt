package de.westnordost.streetcomplete.data.workspace.domain

import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun getWorkspaces() : Flow<List<Workspace>>
    fun loginToWorkspace(username : String, password : String) : Flow<LoginResponse>
}
