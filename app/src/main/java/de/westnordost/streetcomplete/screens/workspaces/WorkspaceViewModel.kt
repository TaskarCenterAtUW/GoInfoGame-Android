package de.westnordost.streetcomplete.screens.workspaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

abstract class WorkspaceViewModel : ViewModel() {
    abstract val showWorkspaces: StateFlow<WorkspaceListState>
    abstract fun loginToWorkspace(
        username: String,
        password: String,
    ): StateFlow<WorkspaceLoginState>
    abstract fun setLoginState(isLoggedIn : Boolean)
    abstract fun setIsLongForm(isLongForm : Boolean)
}

class WorkspaceViewModelImpl(private val workspaceRepository: WorkspaceRepository, private val preferences: Preferences) :
    WorkspaceViewModel() {

        val isLoggedIn : Boolean = preferences.workspaceLogin

    override val showWorkspaces: StateFlow<WorkspaceListState> = flow {
        workspaceRepository.getWorkspaces()
            .catch { e -> emit(WorkspaceListState.error(e.message)) } // Handle errors
            .collect { workspaces ->
                emit(WorkspaceListState.success(workspaces)) // Emit success for each emission
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkspaceListState.loading()
    )

    override fun loginToWorkspace(
        username: String,
        password: String,
    ): StateFlow<WorkspaceLoginState> = flow {
        workspaceRepository.loginToWorkspace(username, password)
            .catch { e -> emit(WorkspaceLoginState.error(e.message)) }
            .collect { loginResponse -> emit(WorkspaceLoginState.success(loginResponse)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkspaceLoginState.loading()
    )

    override fun setLoginState(isLoggedIn: Boolean) {
        preferences.workspaceLogin = isLoggedIn
    }

    override fun setIsLongForm(isLongForm: Boolean) {
        preferences.showLongForm = isLongForm
    }
}
