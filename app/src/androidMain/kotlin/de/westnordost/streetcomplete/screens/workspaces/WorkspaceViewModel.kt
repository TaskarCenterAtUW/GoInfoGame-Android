package de.westnordost.streetcomplete.screens.workspaces

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.streetcomplete.BuildConfig
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.elementfilter.ParseException
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.preferences.Environment
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.data.workspace.domain.model.AppUpdateCheckerResponse
import de.westnordost.streetcomplete.data.workspace.domain.model.LoginResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormResponse
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.WorkspaceDetailsResponse
import de.westnordost.streetcomplete.util.firebase.FirebaseAnalyticsHelper
import de.westnordost.streetcomplete.util.getEmailFromJWT
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

abstract class WorkspaceViewModel : ViewModel() {
    abstract val showWorkspaces: StateFlow<WorkspaceListState>
    abstract fun fetchWorkspaces(location: Location)
    abstract fun refreshWorkspaces()
    abstract fun loginToWorkspace(
        username: String,
        password: String,
    )

    abstract val loginState: StateFlow<WorkspaceLoginState>
    abstract val updateState: StateFlow<AppVersionUpdateState>
    abstract val selectedWorkspace: StateFlow<Workspace?>
    abstract fun getWorkspaceDetails(workspaceId: Int): StateFlow<WorkspaceLongFormState>
    abstract fun setLoginState(isLoggedIn: Boolean, loginResponse: LoginResponse, email: String)
    abstract fun setIsLongForm(isLongForm: Boolean)
    abstract fun setSelectedWorkspace(index: Int)
    abstract fun getUserInfo(email: String)
    abstract fun setEnvironment(environment: Environment)
    abstract fun refreshToken(expediteLogin: Boolean = false)
    abstract fun getAppUpdateInfo()
}

class WorkspaceViewModelImpl(
    private val workspaceRepository: WorkspaceRepository,
    private val preferences: Preferences,
    private val downloadedTilesController: DownloadedTilesController,
) :
    WorkspaceViewModel() {
    val isLoggedIn: Boolean = preferences.workspaceLogin

    private val _selectedWorkspace = MutableStateFlow<Workspace?>(null)
    override val selectedWorkspace: StateFlow<Workspace?> get() = _selectedWorkspace

    private val _loginState = MutableStateFlow<WorkspaceLoginState>(WorkspaceLoginState.Init)
    override val loginState: StateFlow<WorkspaceLoginState> get() = _loginState

    private val _updateState =
        MutableStateFlow<AppVersionUpdateState>(AppVersionUpdateState.Loading)
    override val updateState: StateFlow<AppVersionUpdateState> get() = _updateState

    override fun setSelectedWorkspace(index: Int) {
        _selectedWorkspace.value = (showWorkspaces.value as WorkspaceListState.Success).workspaces
            .filter { it.externalAppAccess == 1 && it.type == "osw" }[index]
        preferences.workspaceId = _selectedWorkspace.value?.id
        // the "has this area already been downloaded" bookkeeping isn't scoped per workspace, so
        // without this, switching workspaces while looking at the same map area makes auto-download
        // wrongly think the new workspace's data is already fresh and skip fetching it
        downloadedTilesController.invalidateAll()
    }

    private var userLocation: Location? = null
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
        userLocation = location
        userLocation?.apply {
            refreshWorkspaces()
        }
    }

    @OptIn(FlowPreview::class)
    override fun refreshWorkspaces() {
        userLocation?.apply {
            viewModelScope.launch {
                _showWorkspaces.value = WorkspaceListState.Loading
                workspaceRepository.getWorkspaces(this@apply)
                    .distinctUntilChanged()
                    .catch { e -> _showWorkspaces.value = WorkspaceListState.error(e.message) }
                    .collect { workspaces ->
                        _showWorkspaces.value = WorkspaceListState.success(workspaces)
                    }
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

    override fun getWorkspaceDetails(workspaceId: Int): StateFlow<WorkspaceLongFormState> = flow {
        emit(WorkspaceLongFormState.loading())
        workspaceRepository.getWorkspaceDetails(workspaceId)
            .catch { e ->
                emit(WorkspaceLongFormState.error(e.message))
                _selectedWorkspace.value = null
            }
            .collect { workspaceDetails ->
                emit(emitLongFormResponse(workspaceDetails))
                _selectedWorkspace.value = null
            }
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

    private fun emitLongFormResponse(workspaceDetails: WorkspaceDetailsResponse): WorkspaceLongFormState {
        try {
            val json = Json {
                ignoreUnknownKeys = true
            }
            val jsonElement = Json.parseToJsonElement("{\"version\":\"3.2.0\",\"recency_period\":90,\"feature-presets\":[{\"name\":\"Street light\",\"icon\":\"street_light\",\"tags\":{\"amenity\":\"street_light\"}},{\"name\":\"Bench\",\"icon\":\"preset_temaki_bench\",\"tags\":{\"amenity\":\"bench\"}},{\"name\":\"Subway\",\"icon\":\"subway\",\"tags\":{\"public_transport\":\"platform\",\"subway\":\"yes\"}}],\"elements\":[{\"element_type\":\"Bench\",\"element_type_icon\":\"bench_poi\",\"quest_query\":\"nodes with (amenity=bench)\",\"quests\":[{\"quest_id\":201,\"quest_title\":\"What is this bench made of?\",\"quest_description\":\"Choose the primary surface material of the sidewalk.\",\"quest_type\":\"ExclusiveChoice\",\"quest_tag\":\"ext:surface\",\"quest_answer_choices\":[{\"value\":\"asphalt\",\"choice_text\":\"Asphalt\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/asphalt_landscape.png\"},{\"value\":\"concrete\",\"choice_text\":\"Concrete\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/concrete_landscape.png\"},{\"value\":\"paving_stones\",\"choice_text\":\"Brick\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/brick_landscape.png\"},{\"value\":\"gravel\",\"choice_text\":\"Gravel\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/compacted_gravel_landscape.png\"},{\"value\":\"other\",\"choice_text\":\"Other\"}]}]},{\"element_type\":\"Sidewalks\",\"element_type_icon\":\"sidewalk\",\"quest_query\":\"ways with (highway=footway and footway=sidewalk)\",\"quests\":[{\"quest_id\":101,\"quest_title\":\"What is this sidewalk's surface type?\",\"quest_description\":\"Choose the primary surface material of the sidewalk.\",\"quest_type\":\"ExclusiveChoice\",\"quest_tag\":\"ext:surface\",\"quest_answer_choices\":[{\"value\":\"asphalt\",\"choice_text\":\"Asphalt\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/asphalt_landscape.png\"},{\"value\":\"concrete\",\"choice_text\":\"Concrete\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/concrete_landscape.png\"},{\"value\":\"paving_stones\",\"choice_text\":\"Brick\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/brick_landscape.png\"},{\"value\":\"gravel\",\"choice_text\":\"Gravel\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/surface/compacted_gravel_landscape.png\"},{\"value\":\"other\",\"choice_text\":\"Other\"}]},{\"quest_id\":102,\"quest_title\":\"Please describe this sidewalk's surface material.\",\"quest_description\":\"Enter a brief description of this sidewalk's surface material.\",\"quest_type\":\"TextEntry\",\"quest_tag\":\"ext:surface:description\",\"quest_image_url\":\"https://provisodevstorage.blob.core.windows.net/projects/gig-element-icons/icons2/sidewalk_surface.png\",\"quest_answer_dependency\":{\"question_id\":101,\"required_value\":\"other\"}},{\"quest_id\":103,\"quest_title\":\"How wide is this sidewalk, in inches?\",\"quest_description\":\"Specify the width of this sidewalk, in inches.\",\"quest_image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/dimension/width_square.png\",\"quest_type\":\"Numeric\",\"quest_tag\":\"width\",\"quest_answer_validation\":{\"min\":12,\"max\":240}},{\"quest_id\":104,\"quest_title\":\"Are there any obstructions along this sidewalk?\",\"quest_description\":\"Check if there are any obstructions blocking this sidewalk.\",\"quest_image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/street_furniture_square.png\",\"quest_type\":\"ExclusiveChoice\",\"quest_tag\":\"ext:obstruction\",\"quest_answer_choices\":[{\"value\":\"yes\",\"choice_text\":\"Yes\"},{\"value\":\"no\",\"choice_text\":\"No\"}]},{\"quest_id\":105,\"quest_title\":\"What types of obstructions are present along this sidewalk?\",\"quest_description\":\"Select all applicable types of obstructions that are present along this sidewalk.\",\"quest_type\":\"MultipleChoice\",\"quest_tag\":\"ext:obstruction:type\",\"quest_answer_dependency\":{\"question_id\":104,\"required_value\":\"yes\"},\"quest_answer_choices\":[{\"value\":\"bollard\",\"choice_text\":\"Bollard\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/bollard_2_square.png\"},{\"value\":\"mailbox\",\"choice_text\":\"Mailbox\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/mailbox_landscape.png\"},{\"value\":\"pole\",\"choice_text\":\"Utility Pole\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/utility_2_square.png\"},{\"value\":\"waste_bin\",\"choice_text\":\"Trash Can\",\"image_url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/waste_bin_square.png\"},{\"value\":\"other\",\"choice_text\":\"Other obstruction\",\"choice_follow_up\":\"Please take a photo of the obstruction.\"}]}]}],\"custom-icons\":[{\"name\":\"street_light\",\"url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/utility_2_square.png\",\"type\":\"feature-preset\"},{\"name\":\"subway\",\"url\":\"https://raw.githubusercontent.com/TaskarCenterAtUW/tdei-tools/main/images/sidewalk/obstruction/mailbox_landscape.png\",\"type\":\"feature-preset\"}]}")
            var featurePresets: List<FeaturePreset> = emptyList()
            var customIcons: List<CustomIcon> = emptyList()
            val longFormResponse = when {
                jsonElement is JsonObject && "version" in jsonElement -> {
                    val wrapper = json.decodeFromJsonElement<LongFormResponse>(jsonElement)
                    if (wrapper.elements.isEmpty()) {
                        return WorkspaceLongFormState.error("No long form quests available for this workspace")
                    }
                    featurePresets = wrapper.featurePresets
                    customIcons = wrapper.customIcons
                    wrapper.elements
                }

                jsonElement is JsonArray -> {
                    if (jsonElement.isEmpty()) {
                        return WorkspaceLongFormState.error("No long form quests available for this workspace")
                    }
                    json.decodeFromJsonElement(jsonElement)
                }

                else -> {
                    return WorkspaceLongFormState.error("Unexpected JSON structure for long form (Null or invalid).  Please contact the admin for this workspace")
                }
            }
            for (item in longFormResponse) {
                item.questQuery?.toElementFilterExpression()
            }
            return WorkspaceLongFormState.success(longFormResponse, workspaceDetails.imageryListDef, featurePresets, customIcons)
        } catch (parseException: ParseException) {
            return WorkspaceLongFormState.error("Workspace is not configured properly. Please contact the admin for this workspace,  " + parseException.message)
        }
    }

    override fun getUserInfo(email: String) {
        viewModelScope.launch {
            workspaceRepository.getUserInfo(email)
                .catch { }
                .collect { response ->
                    preferences.workspaceUserName =
                        "${response.username} \n ${response.firstName} ${response.lastName}"
                    preferences.workspaceUserId = response.id
                    preferences.workspaceUserId?.let {
                        FirebaseAnalyticsHelper.setUserId(it)
                    }
                }
        }
    }

    override fun refreshToken(expediteLogin: Boolean) {
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
                        preferences.workspaceLogin = true
                        preferences.refreshTokenExpiryTime =
                            preferences.workspaceLastLogin + preferences.refreshTokenExpiryInterval
                        preferences.accessTokenExpiryTime =
                            preferences.workspaceLastLogin + preferences.accessTokenExpiryInterval
                        if (expediteLogin) {
                            getEmailFromJWT(loginResponse.access_token)?.let { email ->
                                preferences.workspaceUserEmail = email
                            }
                        }
                        preferences.workspaceUserEmail?.apply {
                            _loginState.value =
                                WorkspaceLoginState.success(loginResponse, this, expediteLogin)
                        } ?: run {
                            _loginState.value = WorkspaceLoginState.error("No user email found")
                        }
                    }
            } ?: run {
                _loginState.value = WorkspaceLoginState.error("No refresh token found")
            }
        }
    }

    override fun setEnvironment(environment: Environment) {
        val environmentManager = EnvironmentManager(preferences)
        environmentManager.currentEnvironment = environment
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

    override fun getAppUpdateInfo() {
        viewModelScope.launch {
            _updateState.value = AppVersionUpdateState.Loading
            workspaceRepository.getAppUpdateInfo()
                .catch { e -> _updateState.value = AppVersionUpdateState.error(e.message) }
                .collect { response ->
                    val environmentManager = EnvironmentManager(preferences)
                    val (isLatestNewer, isForceUpdate) = isLatestVersionNewer(
                        BuildConfig.VERSION_NAME,
                        getVersionForEnvironment(response, environmentManager),
                    )
                    _updateState.value = AppVersionUpdateState.success(
                        isLatestNewer,
                        isForceUpdate,
                        environmentManager.currentEnvironment.firebaseUpdateUrl,
                    )
                }
        }
    }

    private data class VersionInfo(val latestVersion: String, val minimumRequiredVersion: String)

    private fun getVersionForEnvironment(
        response: AppUpdateCheckerResponse,
        environmentManager: EnvironmentManager,
    ): VersionInfo {
        return when (environmentManager.currentEnvironment) {
            Environment.DEV -> VersionInfo(
                response.android.dev.latestVersion,
                response.android.dev.minRequiredVersion
            )

            Environment.STAGE -> VersionInfo(
                response.android.stage.latestVersion,
                response.android.stage.minRequiredVersion
            )

            Environment.PROD -> VersionInfo(
                response.android.prod.latestVersion,
                response.android.prod.minRequiredVersion
            )
        }
    }

    private fun isLatestVersionNewer(
        currentVersion: String,
        versionInfo: VersionInfo,
    ): Pair<Boolean, Boolean> {
        val curParts = currentVersion.split('.')
        val latParts = versionInfo.latestVersion.split('.')
        val minParts = versionInfo.minimumRequiredVersion.split('.')
        val length = maxOf(curParts.size, latParts.size, minParts.size)

        fun parsePart(part: String?): Int {
            if (part == null) return 0
            val digits = Regex("^\\d+").find(part)?.value
            return digits?.toIntOrNull() ?: 0
        }

        fun compareParts(aParts: List<String>, bParts: List<String>, len: Int): Int {
            for (i in 0 until len) {
                val a = parsePart(aParts.getOrNull(i))
                val b = parsePart(bParts.getOrNull(i))
                if (a < b) return -1
                if (a > b) return 1
            }
            return 0
        }

        val latestComparison = compareParts(curParts, latParts, length) // -1 if current < latest
        val minComparison = compareParts(curParts, minParts, length) // -1 if current < minimum

        val isLatestNewer = latestComparison < 0
        val isBelowMinimum = minComparison < 0

        // If current is less than latest -> (true, false).
        // If current is less than latest and also less than minimum -> (true, true).
        // Otherwise -> (false, false).
        return Pair(isLatestNewer, isLatestNewer && isBelowMinimum)
    }
}
