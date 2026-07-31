package de.westnordost.streetcomplete.screens.workspaces

import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.UserInfoResponse
import de.westnordost.streetcomplete.data.workspace.UserProjectGroupItem
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.util.satellite_layers.Imagery

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
    data class Success(
        val longFormItems: List<Elements>,
        val imageryList: List<Imagery>?,
        val recencyPeriodInDays: Int,
        val featurePresets: List<FeaturePreset> = emptyList(),
        val customIcons: List<CustomIcon> = emptyList(),
    ) : WorkspaceLongFormState()

    data class Error(val error: String?) : WorkspaceLongFormState()

    companion object {
        fun loading() = Loading
        fun success(
            workspaces: List<Elements>,
            imageryList: List<Imagery>?,
            recencyPeriodInDays : Int,
            featurePresets: List<FeaturePreset> = emptyList(),
            customIcons: List<CustomIcon> = emptyList(),
        ) = Success(workspaces, imageryList,recencyPeriodInDays, featurePresets, customIcons)

        fun error(errorMessage: String?) = Error(errorMessage ?: "")
    }
}

sealed class WorkspaceProjectGroupsState {
    data object Loading : WorkspaceProjectGroupsState()
    data class Success(val groups: List<UserProjectGroupItem>) : WorkspaceProjectGroupsState()
    data class Error(val error: String?) : WorkspaceProjectGroupsState()

    companion object {
        fun loading() = Loading
        fun success(groups: List<UserProjectGroupItem>) = Success(groups)
        fun error(errorMessage: String?) = Error(errorMessage)
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
    data class Success(val loginResponse: LoginResponse, val email: String, val expediteLogin: Boolean = false) : WorkspaceLoginState()
    data class Error(val error: String?) : WorkspaceLoginState()

    companion object {
        fun Init() = Init
        fun loading() = Loading
        fun success(loginResponse: LoginResponse, email: String, expediteLogin : Boolean = false) = Success(loginResponse, email, expediteLogin)
        fun error(errorMessage: String?) = Error(errorMessage)
    }
}

sealed class AppVersionUpdateState {
    data object Loading : AppVersionUpdateState()
    data class Success(val isUpdateAvailable: Boolean, val isForceUpdate: Boolean, val updateUrl: String) : AppVersionUpdateState()
    data class Error(val error: String?) : AppVersionUpdateState()

    companion object {
        fun loading() = Loading
        fun success(isUpdateAvailable: Boolean, isForceUpdate : Boolean, updateUrl: String) = Success(isUpdateAvailable, isForceUpdate, updateUrl)
        fun error(errorMessage: String?) = Error(errorMessage)
    }
}
