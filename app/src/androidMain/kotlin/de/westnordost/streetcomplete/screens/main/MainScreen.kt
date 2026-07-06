package de.westnordost.streetcomplete.screens.main

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.edithistory.EditKey
import de.westnordost.streetcomplete.data.messages.Message
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNotice
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.overlays.Overlay
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.data.urlconfig.UrlConfig
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.ic_undo_24
import de.westnordost.streetcomplete.resources.location_dot_small
import de.westnordost.streetcomplete.resources.map_attribution_osm
import de.westnordost.streetcomplete.screens.main.controls.AttributionButton
import de.westnordost.streetcomplete.screens.main.controls.AttributionLink
import de.westnordost.streetcomplete.screens.main.controls.CompassButton
import de.westnordost.streetcomplete.screens.main.controls.Crosshair
import de.westnordost.streetcomplete.screens.main.controls.LocationState
import de.westnordost.streetcomplete.screens.main.controls.LocationStateButton
import de.westnordost.streetcomplete.screens.main.controls.MapButton
import de.westnordost.streetcomplete.screens.main.controls.PointerPinButton
import de.westnordost.streetcomplete.screens.main.controls.ScaleBar
import de.westnordost.streetcomplete.screens.main.controls.ZoomButtons
import de.westnordost.streetcomplete.screens.main.controls.findEllipsisIntersection
import de.westnordost.streetcomplete.screens.main.conflicts.DiscardedEditNoticeEffect
import de.westnordost.streetcomplete.screens.main.conflicts.TagConflictResolutionEffect
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistorySidebar
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistoryViewModel
import de.westnordost.streetcomplete.screens.main.edithistory.EditItem
import de.westnordost.streetcomplete.screens.main.errors.LastCrashEffect
import de.westnordost.streetcomplete.screens.main.errors.LastDownloadErrorEffect
import de.westnordost.streetcomplete.screens.main.errors.LastUploadErrorEffect
import de.westnordost.streetcomplete.screens.main.map.maplibre.CameraPosition
import de.westnordost.streetcomplete.screens.main.teammode.TeamModeWizard
import de.westnordost.streetcomplete.screens.main.urlconfig.ApplyUrlConfigEffect
import de.westnordost.streetcomplete.screens.settings.SettingsActivity
import de.westnordost.streetcomplete.ui.common.AnimatedScreenVisibility
import de.westnordost.streetcomplete.ui.common.LargeCreateIcon
import de.westnordost.streetcomplete.ui.common.StopRecordingIcon
import de.westnordost.streetcomplete.ui.ktx.dir
import de.westnordost.streetcomplete.ui.ktx.pxToDp
import de.westnordost.streetcomplete.util.ktx.sendErrorReportEmail
import de.westnordost.streetcomplete.util.ktx.toast
import de.westnordost.streetcomplete.util.satellite_layers.Attribution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs

/** Map controls shown on top of the map. */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    editHistoryViewModel: EditHistoryViewModel,
    onClickZoomIn: () -> Unit,
    onClickZoomOut: () -> Unit,
    onClickCompass: () -> Unit,
    onClickLocation: () -> Unit,
    onClickLocationPointer: () -> Unit,
    onClickCreate: () -> Unit,
    onClickStopTrackRecording: () -> Unit,
    onClickDownload: () -> Unit,
    onClickImageryLayer: () -> Unit,
    onSwitchWorkspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val starsCount by viewModel.starsCount.collectAsState()
    val isShowingStarsCurrentWeek by viewModel.isShowingStarsCurrentWeek.collectAsState()

    val overlays by viewModel.overlays.collectAsState()
    val selectedOverlay by viewModel.selectedOverlay.collectAsState()
    val isCreateNodeEnabled by remember { derivedStateOf { selectedOverlay?.isCreateNodeEnabled == true } }

    val isAutoSync by viewModel.isAutoSync.collectAsState()
    val unsyncedEditsCount by viewModel.unsyncedEditsCount.collectAsState()

    val isTeamMode by viewModel.isTeamMode.collectAsState()
    val indexInTeam by viewModel.indexInTeam.collectAsState()

    val messagesCount by viewModel.messagesCount.collectAsState()
    val hasMessages by remember { derivedStateOf { messagesCount > 0 } }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isUploadingOrDownloading by viewModel.isUploadingOrDownloading.collectAsState()

    val urlConfig by viewModel.urlConfig.collectAsState()
    val lastCrashReport by viewModel.lastCrashReport.collectAsState()
    val lastDownloadError by viewModel.lastDownloadError.collectAsState()
    val lastUploadError by viewModel.lastUploadError.collectAsState()

    val locationState by viewModel.locationState.collectAsState()
    val isNavigationMode by viewModel.isNavigationMode.collectAsState()
    val isFollowingPosition by viewModel.isFollowingPosition.collectAsState()
    val isRecordingTracks by viewModel.isRecordingTracks.collectAsState()
    val userHasMovedCamera by viewModel.userHasMovedCamera.collectAsState()

    val mapCamera by viewModel.mapCamera.collectAsState()
    val metersPerDp by viewModel.metersPerDp.collectAsState()
    val displayedPosition by viewModel.displayedPosition.collectAsState()

    val editItems by editHistoryViewModel.editItems.collectAsState()
    val selectedEdit by editHistoryViewModel.selectedEdit.collectAsState()
    val hasEdits by remember { derivedStateOf { editItems.isNotEmpty() } }

    val showZoomButtons by viewModel.showZoomButtons.collectAsState()
    val pendingConflictsCount by viewModel.pendingConflictsCount.collectAsState()
    val discardedNoticesCount by viewModel.discardedNoticesCount.collectAsState()

    var showOverlaysDropdown by remember { mutableStateOf(false) }
    var showTeamModeWizard by remember { mutableStateOf(false) }
    val showMainMenuDialog by viewModel.showMainMenuDialog.collectAsState()
    var shownMessage by remember { mutableStateOf<Message?>(null) }
    val showEditHistorySidebar by editHistoryViewModel.isShowingSidebar.collectAsState()

    val mapRotation = mapCamera?.rotation ?: 0.0
    val mapTilt = mapCamera?.tilt ?: 0.0

    val mapAttribution = mutableListOf(
        AttributionLink(
            stringResource(Res.string.map_attribution_osm),
            "https://osm.org/copyright"
        ),
        AttributionLink("© JawgMaps", "https://jawg.io"),
    )

    fun onClickOverlays() {
        onClickImageryLayer()
    }

    fun onClickMessages() {
        scope.launch {
            shownMessage = viewModel.popMessage()
        }
    }

    fun onClickUpload() {
        if (viewModel.isConnected) {
            viewModel.upload()
        } else {
            context.toast(R.string.offline)
        }
    }

    fun sendErrorReport(error: Exception) {
        scope.launch {
            val report = viewModel.createErrorReport(error)
            context.sendErrorReportEmail(report)
        }
    }

    LaunchedEffect(isTeamMode) {
        // always show this toast on start to remind user that it is still on
        if (isTeamMode) {
            context.toast(R.string.team_mode_active)
        }
        // show this only once when turning it off
        else if (viewModel.teamModeChanged) {
            context.toast(R.string.team_mode_deactivated)
            viewModel.teamModeChanged = false
        }
    }

    Box(modifier) {
        if (isCreateNodeEnabled) {
            Crosshair()
        }

        var screen by remember { mutableStateOf<Rect?>(null) }
        val intersection = remember(displayedPosition, screen) {
            findEllipsisIntersection(screen, displayedPosition)
        }

        intersection?.let { (offset, angle) ->
            val rotation = angle * 180 / PI
            PointerPinButton(
                onClick = onClickLocationPointer,
                rotate = rotation.toFloat(),
                modifier = Modifier.absoluteOffset(offset.x.pxToDp(), offset.y.pxToDp()),
            ) { Image(painterResource(Res.drawable.location_dot_small), null) }
        }

        Column(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { screen = it.boundsInRoot() }
        ) {

            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .onGloballyPositioned { screen = it.boundsInRoot() }
            ) {
                // Column {
                //     WorkspaceCard(
                //         modifier = Modifier
                //             .align(Alignment.CenterHorizontally),
                //         workspaceName = viewModel.workspaceTitle.collectAsState().value,
                //         pendingCount = unsyncedEditsCount,
                //         onRefreshClick = {
                //             if (unsyncedEditsCount > 0) {
                //                 if (viewModel.isConnected) {
                //                     viewModel.upload()
                //                 } else {
                //                     context.toast(R.string.offline)
                //                 }
                //             }
                //         },
                //         onLayersClick = onClickImageryLayer,
                //         onMenuClick = { showMainMenuDialog = true },
                //         onProfileClick = {
                //             context.startActivity(Intent(context, UserActivity::class.java))
                //         },
                //         showProgress = isUploadingOrDownloading,
                //     )
                // }

                // // top-start controls
                // Box(Modifier.align(Alignment.TopStart)) {
                //     // stars counter
                //     if (isUploadingOrDownloading)
                //         CircularProgressIndicator(
                //             modifier = Modifier.size(48.dp),
                //             color = MaterialTheme.colorScheme.secondary
                //         )
                // }

                // top-end controls
                // Row(
                //     modifier = Modifier
                //         .align(Alignment.TopEnd)
                //         .padding(4.dp),
                //     horizontalArrangement = Arrangement.spacedBy(8.dp)
                // ) {
                //     if (overlays.isNotEmpty()) {
                //         Box {
                //             OverlaySelectionButton(
                //                 onClick = ::onClickOverlays,
                //                 overlay = selectedOverlay
                //             )
                //             OverlaySelectionDropdownMenu(
                //                 expanded = showOverlaysDropdown,
                //                 onDismissRequest = { showOverlaysDropdown = false },
                //                 overlays = overlays,
                //                 onSelect = { viewModel.selectOverlay(it) }
                //             )
                //         }
                //     }
                //
                //     MainMenuButton(
                //         onClick = { showMainMenuDialog = true },
                //         unsyncedEditsCount = if (!isAutoSync) unsyncedEditsCount else 0,
                //         indexInTeam = if (isTeamMode) indexInTeam else null
                //     )
                // }

                // bottom controls
                Column(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .semantics { isTraversalGroup = true }
                    ) {
                        // bottom-end controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .semantics { isTraversalGroup = true },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            val isCompassVisible = abs(mapRotation) >= 1.0 || abs(mapTilt) >= 1.0
                            AnimatedVisibility(
                                visible = isCompassVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                CompassButton(
                                    onClick = onClickCompass,
                                    modifier = Modifier.graphicsLayer(
                                        rotationZ = -mapRotation.toFloat(),
                                        rotationX = mapTilt.toFloat()
                                    )
                                )
                            }
                            if (showZoomButtons) {
                                ZoomButtons(
                                    onZoomIn = onClickZoomIn,
                                    onZoomOut = onClickZoomOut,
                                    modifier = Modifier.semantics(mergeDescendants = true) {
                                        // This provides a "flat" string for TalkBack to read
                                        // while the visual remains styled.
                                        traversalIndex = 1f
                                    }
                                )
                            }
                            LocationStateButton(
                                onClick = onClickLocation,
                                state = locationState,
                                isNavigationMode = isNavigationMode,
                                isFollowing = isFollowingPosition,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    // This provides a "flat" string for TalkBack to read
                                    // while the visual remains styled.
                                    traversalIndex = 2f
                                }
                            )
                        }

                        if (isCreateNodeEnabled) {
                            MapButton(
                                onClick = {
                                    if ((mapCamera?.zoom ?: 0.0) >= 17.0) {
                                        onClickCreate()
                                    } else {
                                        context.toast(
                                            R.string.download_area_too_big,
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .align(BiasAlignment(0.333f, 1f))
                                    .padding(4.dp),
                            ) {
                                LargeCreateIcon()
                            }
                        }

                        // bottom-start controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRecordingTracks) {
                                MapButton(
                                    onClick = onClickStopTrackRecording,

                                    ) {
                                    StopRecordingIcon()
                                }
                            }

                            if (hasEdits) {
                                MapButton(
                                    onClick = { editHistoryViewModel.showSidebar() },
                                    // Don't allow undoing while uploading. Should prevent race conditions.
                                    // (Undoing quest while also uploading it at the same time)
                                    enabled = !isUploadingOrDownloading,
                                    modifier = Modifier.semantics(mergeDescendants = true) {
                                        // This provides a "flat" string for TalkBack to read
                                        // while the visual remains styled.
                                        contentDescription = "Undo Edits Button"
                                        traversalIndex = 0f
                                    }) {
                                    Icon(painterResource(Res.drawable.ic_undo_24), null)
                                }
                            }
                        }
                    }
                    // Alternative to this would be to put the tutorial screens into a separate
                    // navigation destination in a TBD MainNavHost after complete migration to Compose
                    // (see #6255)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clearAndSetSemantics {

                            }
                    ) {
                        val attributions = viewModel.attribution
                        attributions.collectAsState().value?.let { it ->
                            mapAttribution.add(
                                AttributionLink(
                                    ensureCopyright(it.text), it.url
                                )
                            )
                        }
                        AttributionButton(
                            userHasMovedMap = userHasMovedCamera,
                            attributions = mapAttribution,
                            modifier = Modifier
                                .align(Alignment.TopStart).clearAndSetSemantics{},
                            popupElevation = 4.dp,
                        )
                        ScaleBar(
                            metersPerDp = metersPerDp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(horizontal = 12.dp),
                            alignment = Alignment.End,
                        )
                    }
                }
            }
        }

        val dir = LocalLayoutDirection.current.dir
        AnimatedVisibility(
            visible = showEditHistorySidebar,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 2 * dir }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 2 * dir }),
        ) {
            EditHistorySidebar(
                editItems = editItems,
                selectedEdit = selectedEdit,
                onSelectEdit = { editHistoryViewModel.select(it.key) },
                onUndoEdit = { editHistoryViewModel.undo(it.key) },
                onDismissRequest = { editHistoryViewModel.hideSidebar() },
                featureDictionaryLazy = editHistoryViewModel.featureDictionaryLazy,
                getEditElement = editHistoryViewModel::getEditElement,
            )
        }
    }

    if (showMainMenuDialog) {
        MainMenuDialog(
            onDismissRequest = { viewModel.hideMenu() },
            onClickSettings = {
                context.startActivity(
                    Intent(

                        context,
                        SettingsActivity::class.java
                    )
                )
            },
            onClickDownload = onClickDownload,
            onSwitchWorkspace = onSwitchWorkspace
        )
    }

    urlConfig?.let { config ->
        ApplyUrlConfigEffect(
            urlConfig = config.urlConfig,
            presetNameAlreadyExists = config.alreadyExists,
            onApplyUrlConfig = { viewModel.applyUrlConfig(it) }
        )
    }
    lastDownloadError?.let { error ->
        LastDownloadErrorEffect(lastError = error, onReportError = ::sendErrorReport)
    }
    lastUploadError?.let { error ->
        LastUploadErrorEffect(lastError = error, onReportError = ::sendErrorReport)
    }
    TagConflictResolutionEffect(
        pendingConflictsCount = pendingConflictsCount,
        onPopNextConflict = { viewModel.popNextConflict() },
        onResolveKeepMine = { viewModel.resolveConflictKeepMine(it) },
        onResolveKeepTheirs = { viewModel.resolveConflictKeepTheirs(it) }
    )
    DiscardedEditNoticeEffect(
        discardedNoticesCount = discardedNoticesCount,
        onPopNextDiscardedNotice = { viewModel.popNextDiscardedNotice() },
        onDismissDiscardedNotice = { viewModel.dismissDiscardedNotice(it) }
    )
    lastCrashReport?.let { report ->
        LastCrashEffect(lastReport = report, onReport = { context.sendErrorReportEmail(it) })
    }

    AnimatedScreenVisibility(showTeamModeWizard) {
        val questIcons = remember { viewModel.allQuestTypes.map { it.icon } }
        TeamModeWizard(
            onDismissRequest = { showTeamModeWizard = false },
            onFinished = { teamSize, indexInTeam ->
                viewModel.enableTeamMode(
                    teamSize = teamSize,
                    indexInTeam = indexInTeam
                )
            },
            allQuestIconIds = questIcons
        )
    }
}

fun ensureCopyright(text: String): String {
    return if (text.contains("©")) {
        text  // already has copyright symbol
    } else {
        "\u00A9 $text"  // prepend ©
    }
}

@Preview
@Composable
private fun PreviewMainScreen() {

    MainScreen(
        viewModel = PreviewMainViewModel,
        editHistoryViewModel = PreviewEditHistoryViewModel,
        onClickZoomIn = {},
        onClickZoomOut = {},
        onClickCompass = {},
        onClickLocation = {},
        onClickLocationPointer = {},
        onClickCreate = {},
        onClickStopTrackRecording = {},
        onClickDownload = {},
        onClickImageryLayer = {},
        onSwitchWorkspace = {}
    )
}

object PreviewEditHistoryViewModel : EditHistoryViewModel() {
    override val editItems: StateFlow<List<EditItem>>
        get() = MutableStateFlow(emptyList())
    override val selectedEdit: StateFlow<Edit?>
        get() = MutableStateFlow(null)

    override suspend fun getEditElement(edit: Edit): Element? {
        TODO("Not yet implemented")
    }

    override suspend fun getEditGeometry(edit: Edit): ElementGeometry {
        TODO("Not yet implemented")
    }

    override fun select(editKey: EditKey?) {
        TODO("Not yet implemented")
    }

    override fun undo(editKey: EditKey) {
        TODO("Not yet implemented")
    }

    override fun refreshForNewWorkspace() {
        TODO("Not yet implemented")
    }

    override val featureDictionaryLazy: Lazy<FeatureDictionary>
        get() = TODO("Not yet implemented")

    override fun showSidebar() {
        TODO("Not yet implemented")
    }

    override fun hideSidebar() {
        TODO("Not yet implemented")
    }

    override val isShowingSidebar: StateFlow<Boolean>
        get() = MutableStateFlow(false)
}

object PreviewMainViewModel : MainViewModel() {
    override val lastCrashReport: StateFlow<String?>
        get() = MutableStateFlow(null)
    override val lastDownloadError: StateFlow<Exception?>
        get() = MutableStateFlow(null)
    override val lastUploadError: StateFlow<Exception?>
        get() = MutableStateFlow(null)

    override suspend fun createErrorReport(error: Exception): String {
        TODO("Not yet implemented")
    }

    override fun setUri(uri: String) {
        TODO("Not yet implemented")
    }

    override val urlConfig: StateFlow<ShownUrlConfig?>
        get() = MutableStateFlow(null)

    override fun applyUrlConfig(config: UrlConfig) {
        TODO("Not yet implemented")
    }

    override val geoUri: StateFlow<CameraPosition?>
        get() = TODO("Not yet implemented")

    override fun consumeGeoUri() {
        TODO("Not yet implemented")
    }

    override var showZoomButtons: StateFlow<Boolean>
        get() = MutableStateFlow(false)
        set(value) {}
    override val messagesCount: StateFlow<Int>
        get() = MutableStateFlow(1)

    override suspend fun popMessage(): Message? {
        TODO("Not yet implemented")
    }

    override val allQuestTypes: List<QuestType>
        get() = TODO("Not yet implemented")
    override val selectedOverlay: StateFlow<Overlay?>
        get() = MutableStateFlow(null)
    override val overlays: StateFlow<List<Overlay>>
        get() = MutableStateFlow(emptyList())

    override fun selectOverlay(overlay: Overlay?) {
        TODO("Not yet implemented")
    }

    override val isTeamMode: StateFlow<Boolean>
        get() = MutableStateFlow(false)
    override var teamModeChanged: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override val indexInTeam: StateFlow<Int>
        get() = MutableStateFlow(2)

    override fun enableTeamMode(teamSize: Int, indexInTeam: Int) {
        TODO("Not yet implemented")
    }

    override fun disableTeamMode() {
        TODO("Not yet implemented")
    }

    override val isAutoSync: StateFlow<Boolean>
        get() = MutableStateFlow(true)
    override val unsyncedEditsCount: StateFlow<Int>
        get() = MutableStateFlow(2)
    override val isUploading: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    override val isUploadingOrDownloading: StateFlow<Boolean>
        get() = MutableStateFlow(true)
    override val pendingConflictsCount: StateFlow<Int>
        get() = MutableStateFlow(0)
    override suspend fun popNextConflict(): PendingTagConflict? = null
    override suspend fun resolveConflictKeepMine(conflict: PendingTagConflict) {}
    override suspend fun resolveConflictKeepTheirs(conflict: PendingTagConflict) {}
    override val discardedNoticesCount: StateFlow<Int>
        get() = MutableStateFlow(0)
    override suspend fun popNextDiscardedNotice(): DiscardedEditNotice? = null
    override suspend fun dismissDiscardedNotice(notice: DiscardedEditNotice) {}
    override val isUserInitiatedDownloadInProgress: Boolean
        get() = TODO("Not yet implemented")
    override val isLoggedIn: StateFlow<Boolean>
        get() = MutableStateFlow(true)
    override val isConnected: Boolean
        get() = TODO("Not yet implemented")
    override val isRequestingLogin: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    override fun finishRequestingLogin() {
        TODO("Not yet implemented")
    }

    override fun upload() {
        TODO("Not yet implemented")
    }

    override fun download(bbox: BoundingBox) {
        TODO("Not yet implemented")
    }

    override val starsCount: StateFlow<Int>
        get() = MutableStateFlow(2)
    override var attribution: MutableStateFlow<Attribution?>
        get() = MutableStateFlow(null)
        set(value) {}
    override val isShowingStarsCurrentWeek: StateFlow<Boolean>
        get() = MutableStateFlow(true)

    override fun toggleShowingCurrentWeek() {
        TODO("Not yet implemented")
    }

    override val locationState: MutableStateFlow<LocationState>
        get() = MutableStateFlow(LocationState.ALLOWED)
    override val mapCamera: MutableStateFlow<CameraPosition?>
        get() = MutableStateFlow(null)
    override val metersPerDp: MutableStateFlow<Double>
        get() = MutableStateFlow(0.0)
    override val displayedPosition: MutableStateFlow<Offset?>
        get() = MutableStateFlow(null)
    override val isFollowingPosition: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)
    override val isNavigationMode: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)
    override val isRecordingTracks: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)
    override val userHasMovedCamera: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)

    override fun addAttributionsToMap(attribution: Attribution?) {
        TODO("Not yet implemented")
    }

    override var workspaceTitle: MutableStateFlow<String>
        get() = MutableStateFlow("Preview Workspace")
        set(value) {}

    override val showMainMenuDialog: MutableStateFlow<Boolean>
        get() = MutableStateFlow(false)

    override fun showMenu() {
        TODO("Not yet implemented")
    }

    override fun hideMenu() {
        TODO("Not yet implemented")
    }

    override val followVisible: MutableStateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override val undoVisible: MutableStateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override val refreshCounter: MutableStateFlow<Int>
        get() = TODO("Not yet implemented")

    override fun triggerRefresh() {
        TODO("Not yet implemented")
    }
}
