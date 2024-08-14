package de.westnordost.streetcomplete.screens.workspaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.osmapi.OsmConnection
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WorkspaceViewModel : ViewModel() {
    abstract val showWorkspaces: StateFlow<WorkspaceListState>
    abstract fun loginToWorkspace(
        username: String,
        password: String,
    )
    abstract val loginState: StateFlow<WorkspaceLoginState>
    abstract val selectedWorkspace: StateFlow<Int?>
    abstract fun getLongForm(workspaceId: Int): StateFlow<WorkspaceLongFormState>
    abstract fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse)
    abstract fun setIsLongForm(isLongForm: Boolean)
    abstract fun setSelectedWorkspace(workspaceId: Int)
}

class WorkspaceViewModelImpl(
    private val workspaceRepository: WorkspaceRepository,
    private val preferences: Preferences
) :
    WorkspaceViewModel() {
    val isLoggedIn: Boolean = preferences.workspaceLogin

    private val _selectedWorkspace = MutableStateFlow<Int?>(null)
    override val selectedWorkspace: StateFlow<Int?> get() = _selectedWorkspace

    private val _loginState = MutableStateFlow<WorkspaceLoginState>(WorkspaceLoginState.Init)
    override val loginState: StateFlow<WorkspaceLoginState> get() = _loginState

    override fun setSelectedWorkspace(workspaceId: Int) {
        _selectedWorkspace.value = workspaceId
        preferences.workspaceId = workspaceId
    }

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

    override fun loginToWorkspace(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = WorkspaceLoginState.loading()
            workspaceRepository.loginToWorkspace(username, password)
                .catch { e -> _loginState.value = WorkspaceLoginState.error(e.message) }
                .collect { loginResponse -> _loginState.value = WorkspaceLoginState.success(loginResponse) }
        }
    }

    override fun getLongForm(workspaceId: Int): StateFlow<WorkspaceLongFormState> = flow {
        workspaceRepository.getLongFormForWorkspace(workspaceId)
            .catch { e -> emit(WorkspaceLongFormState.error(e.message)) }
            .collect { loginResponse -> emit(WorkspaceLongFormState.success(loginResponse)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkspaceLongFormState.loading()
    )

    override fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse) {
        preferences.workspaceLogin = isLoggedIn
        preferences.workspaceToken = loginResponse.access_token
    }

    override fun setIsLongForm(isLongForm: Boolean) {
        preferences.showLongForm = isLongForm
    }
}
