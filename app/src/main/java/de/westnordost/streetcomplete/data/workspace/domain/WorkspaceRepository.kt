package de.westnordost.streetcomplete.data.workspace.domain

import android.location.Location
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun getWorkspaces(location: Location) : Flow<List<Workspace>>
    fun getLongFormForWorkspace(workspaceId : Int) : Flow<List<AddLongFormResponseItem>>
    fun loginToWorkspace(username : String, password : String) : Flow<LoginResponse>
    suspend fun getUserInfo(userEmail : String) : UserInfoResponse
    fun refreshToken(refreshToken : String) : Flow<LoginResponse>
}
