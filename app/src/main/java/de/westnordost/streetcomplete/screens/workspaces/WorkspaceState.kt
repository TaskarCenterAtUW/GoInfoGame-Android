package de.westnordost.streetcomplete.screens.workspaces

import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem

sealed class WorkspaceListState {
    data object Loading : WorkspaceListState()
    data class Success(val workspaces: List<Workspace>) : WorkspaceListState()
    data class Error(val error: String?) : WorkspaceListState()

    companion object {
        fun loading() = Loading
        fun success(workspaces: List<Workspace>) = Success(workspaces)
        fun error(errorMessage: String?) = Error(errorMessage ?: "")
    }
}

sealed class WorkspaceLongFormState {
    data object Loading : WorkspaceLongFormState()
    data class Success(val longFormItems: List<AddLongFormResponseItem>) : WorkspaceLongFormState()
    data class Error(val error: String?) : WorkspaceLongFormState()

    companion object {
        fun loading() = Loading
        fun success(workspaces: List<AddLongFormResponseItem>) = Success(workspaces)
        fun error(errorMessage: String?) = Error(errorMessage ?: "")
    }
}

sealed class WorkspaceUserInfoState {
    data object Loading : WorkspaceUserInfoState()
    data class Success(val userInfoResponse: UserInfoResponse) : WorkspaceUserInfoState()
    data class Error(val error: String?) : WorkspaceUserInfoState()

    companion object {
        fun loading() = Loading
        fun success(userInfo: UserInfoResponse) = Success(userInfo)
        fun error(errorMessage: String?) = Error(errorMessage)
    }
}

sealed class WorkspaceLoginState {
    data object Init : WorkspaceLoginState()
    data object Loading : WorkspaceLoginState()
    data class Success(val loginResponse: LoginResponse,val email : String) : WorkspaceLoginState()
    data class Error(val error: String?) : WorkspaceLoginState()

    companion object {
        fun Init() = Init
        fun loading() = Loading
        fun success(loginResponse: LoginResponse, email: String) = Success(loginResponse, email)
        fun error(errorMessage: String?) = Error(errorMessage)
    }
}
