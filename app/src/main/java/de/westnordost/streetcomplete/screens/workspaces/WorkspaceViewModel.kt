package de.westnordost.streetcomplete.screens.workspaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.Environment
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
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
    abstract fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse, email : String)
    abstract fun setIsLongForm(isLongForm: Boolean)
    abstract fun setSelectedWorkspace(workspaceId: Int)
    abstract fun getUserInfo(email : String)
    abstract fun setEnvironment(environment: Environment)
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
                .collect { loginResponse -> _loginState.value = WorkspaceLoginState.success(loginResponse, username) }
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

    override fun getUserInfo(email: String) {
        viewModelScope.launch {
            val response = workspaceRepository.getUserInfo(email)
            response.let {
                preferences.workspaceUserName = "${response.username} \n ${response.firstName} ${response.lastName}"
            }
        }
    }

    override fun setEnvironment(environment: Environment) {
        EnvironmentManager(preferences).currentEnvironment = environment
    }

    override fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse, email: String) {
        preferences.workspaceLogin = isLoggedIn
        preferences.workspaceToken = loginResponse.access_token
        getUserInfo(email)
    }

    override fun setIsLongForm(isLongForm: Boolean) {
        preferences.showLongForm = isLongForm
    }
}
