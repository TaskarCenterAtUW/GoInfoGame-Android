package de.westnordost.streetcomplete.screens.workspaces

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.streetcomplete.data.elementfilter.ParseException
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.Environment
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WorkspaceViewModel : ViewModel() {
    abstract val showWorkspaces: StateFlow<WorkspaceListState>
    abstract fun fetchWorkspaces(location: Location)
    abstract fun loginToWorkspace(
        username: String,
        password: String,
    )

    abstract val loginState: StateFlow<WorkspaceLoginState>
    abstract val selectedWorkspace: StateFlow<Int?>
    abstract fun getLongForm(workspaceId: Int): StateFlow<WorkspaceLongFormState>
    abstract fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse, email: String)
    abstract fun setIsLongForm(isLongForm: Boolean)
    abstract fun setSelectedWorkspace(workspaceId: Int)
    abstract fun getUserInfo(email: String)
    abstract fun setEnvironment(environment: Environment)
    abstract fun refreshToken()
}

class WorkspaceViewModelImpl(
    private val workspaceRepository: WorkspaceRepository,
    private val preferences: Preferences,
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

    private val _showWorkspaces = MutableStateFlow<WorkspaceListState>(WorkspaceListState.Loading)
    override val showWorkspaces: StateFlow<WorkspaceListState> get() = _showWorkspaces

    // override val showWorkspaces: StateFlow<WorkspaceListState> = flow {
    //     workspaceRepository.getWorkspaces()
    //         .catch { e -> emit(WorkspaceListState.error(e.message)) } // Handle errors
    //         .collect { workspaces ->
    //             emit(WorkspaceListState.success(workspaces)) // Emit success for each emission
    //         }
    // }.stateIn(
    //     scope = viewModelScope,
    //     started = SharingStarted.WhileSubscribed(5000),
    //     initialValue = WorkspaceListState.loading()
    // )

    @OptIn(FlowPreview::class)
    override fun fetchWorkspaces(location: Location) {
        viewModelScope.launch {
            _showWorkspaces.value = WorkspaceListState.Loading
            workspaceRepository.getWorkspaces(location)
                .debounce(300)
                .distinctUntilChanged()
                .catch { e -> _showWorkspaces.value = WorkspaceListState.error(e.message) }
                .collect { workspaces ->
                    _showWorkspaces.value = WorkspaceListState.success(workspaces)
                }
        }
    }

    override fun loginToWorkspace(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = WorkspaceLoginState.loading()
            workspaceRepository.loginToWorkspace(username, password)
                .catch { e -> _loginState.value = WorkspaceLoginState.error(e.message) }
                .collect { loginResponse ->
                    _loginState.value = WorkspaceLoginState.success(loginResponse, username)
                }
        }
    }

    override fun getLongForm(workspaceId: Int): StateFlow<WorkspaceLongFormState> = flow {
        emit(WorkspaceLongFormState.loading())
        workspaceRepository.getLongFormForWorkspace(workspaceId)
            .catch { e -> emit(WorkspaceLongFormState.error(e.message))
                _selectedWorkspace.value = null}
            .collect { longFormResponse -> emit(emitLongFormResponse(longFormResponse))
                _selectedWorkspace.value = null}
//            .collect { longFormResponse -> if (isValidLongForm(longFormResponse)) {
//                emit(WorkspaceLongFormState.success(longFormResponse))
//            } else {
//                emit(WorkspaceLongFormState.error("Invalid login response"))
//            } }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkspaceLongFormState.loading()
    )

    private fun emitLongFormResponse(longFormResponse: List<AddLongFormResponseItem>): WorkspaceLongFormState {
        try {
            for (item in longFormResponse) {
                item.questQuery?.toElementFilterExpression()
            }
            return WorkspaceLongFormState.success(longFormResponse)
        } catch (parseException: ParseException) {
            return WorkspaceLongFormState.error("Invalid quest filter " + parseException.message)
        }
    }

    override fun getUserInfo(email: String) {
        viewModelScope.launch {
            val response = workspaceRepository.getUserInfo(email)
            response.let {
                preferences.workspaceUserName =
                    "${response.username} \n ${response.firstName} ${response.lastName}"
            }
        }
    }

    override fun refreshToken() {
        viewModelScope.launch {
            _loginState.value = WorkspaceLoginState.loading()
            preferences.workspaceRefreshToken?.let {
                workspaceRepository.refreshToken(it)
                    .catch { e -> _loginState.value = WorkspaceLoginState.error(e.message) }
                    .collect { loginResponse ->
                        preferences.workspaceToken = loginResponse.access_token
                        preferences.workspaceRefreshToken = loginResponse.refresh_token
                        preferences.refreshTokenExpiryInterval =
                            loginResponse.refresh_expires_in * 1000
                        preferences.accessTokenExpiryInterval = loginResponse.expires_in * 1000
                        preferences.workspaceLastLogin = System.currentTimeMillis()

                        preferences.refreshTokenExpiryTime =
                            preferences.workspaceLastLogin + preferences.refreshTokenExpiryInterval
                        preferences.accessTokenExpiryTime =
                            preferences.workspaceLastLogin + preferences.accessTokenExpiryInterval

                        preferences.workspaceUserEmail?.apply {
                            _loginState.value = WorkspaceLoginState.success(loginResponse, this)
                        }
                    }
            } ?: run {
                _loginState.value = WorkspaceLoginState.error("No refresh token found")
            }
        }
    }

    override fun setEnvironment(environment: Environment) {
        EnvironmentManager(preferences).currentEnvironment = environment
    }

    override fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse, email: String) {
        preferences.workspaceLogin = isLoggedIn
        preferences.workspaceToken = loginResponse.access_token
        preferences.workspaceRefreshToken = loginResponse.refresh_token
        preferences.workspaceUserEmail = email
        preferences.refreshTokenExpiryInterval = loginResponse.refresh_expires_in * 1000
        preferences.accessTokenExpiryInterval = loginResponse.expires_in * 1000
        preferences.workspaceLastLogin = System.currentTimeMillis()

        preferences.refreshTokenExpiryTime =
            preferences.workspaceLastLogin + preferences.refreshTokenExpiryInterval
        preferences.accessTokenExpiryTime =
            preferences.workspaceLastLogin + preferences.accessTokenExpiryInterval
        getUserInfo(email)
    }

    override fun setIsLongForm(isLongForm: Boolean) {
        preferences.showLongForm = isLongForm
    }
}
