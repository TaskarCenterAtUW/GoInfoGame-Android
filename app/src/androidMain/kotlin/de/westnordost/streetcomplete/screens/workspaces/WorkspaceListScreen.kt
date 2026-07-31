package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.screens.main.MainActivity
import de.westnordost.streetcomplete.screens.user.UserActivity
import de.westnordost.streetcomplete.ui.common.UserInitialsAvatar
import de.westnordost.streetcomplete.ui.theme.ProximaNovaFontFamily
import de.westnordost.streetcomplete.util.satellite_layers.Imagery

@Composable
fun WorkSpaceListScreen(
    viewModel: WorkspaceViewModel,
    preferences: Preferences,
    modifier: Modifier = Modifier,
) {
    val workspaceListState by viewModel.showWorkspaces.collectAsState()
    val projectGroupsState by viewModel.projectGroupsState.collectAsState()
    val userProjectGroups =
        (projectGroupsState as? WorkspaceProjectGroupsState.Success)?.groups ?: emptyList()
    val projectGroupNames = remember(userProjectGroups) {
        userProjectGroups.associate { it.tdeiProjectGroupId to it.projectGroupName }
    }
    var isLoading by remember { mutableStateOf(false) }
    var isLongFormLoading by remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    var snackBarMessage by remember { mutableStateOf<String?>(null) }
    // what "Refresh" on the snackbar actually retries - set alongside snackBarMessage by
    // whichever of the three error sources below fired, so it retries the thing that actually
    // failed instead of always just refetching the workspace list
    var retryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    val onClick: (workspace: Workspace) -> Unit = { workspace ->
        viewModel.setSelectedWorkspace(workspace)
    }

    val eligibleWorkspaces = (workspaceListState as? WorkspaceListState.Success)?.workspaces
        ?.filter { it.externalAppAccess == 1 && it.type == "osw" } ?: emptyList()
    val hasWorkspaces = eligibleWorkspaces.isNotEmpty()

    var selectedProjectGroup by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val projectGroups = remember(eligibleWorkspaces) {
        eligibleWorkspaces.mapNotNull { it.tdeiProjectGroupId }.distinct().sorted()
    }
    val visibleWorkspaces = remember(eligibleWorkspaces, selectedProjectGroup, searchQuery) {
        eligibleWorkspaces
            .filter { selectedProjectGroup == null || it.tdeiProjectGroupId == selectedProjectGroup }
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            .sortedByDescending { it.createdAt ?: "" }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // toolbar (and its profile avatar -> logout path) stays visible in every state -
        // loading, empty, or error - so the user always has somewhere to go; search only makes
        // sense once there's something to search through
        Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceToolbar(
                onProfileClick = {
                    context.startActivity(Intent(context, UserActivity::class.java))
                },
                userName = preferences.workspaceUserName?.split("\n")?.getOrNull(1)?.trim(),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                showSearch = hasWorkspaces,
            )

            when (workspaceListState) {
                is WorkspaceListState.Loading -> {
                    isLoading = true
                }

                is WorkspaceListState.Success -> {
                    isLoading = false
                    if (!hasWorkspaces) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workspaces available in your area",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        Column(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)) {
                            WorkspaceList(
                                onClick,
                                modifier = Modifier,
                                items = visibleWorkspaces,
                                viewModel = viewModel,
                                projectGroups = projectGroups,
                                projectGroupNames = projectGroupNames,
                                selectedProjectGroup = selectedProjectGroup,
                                onProjectGroupSelected = { selectedProjectGroup = it },
                            )
                        }
                    }
                }

                is WorkspaceListState.Error -> {
                    isLoading = false
                    val error = (workspaceListState as WorkspaceListState.Error).error
                    snackBarMessage = "Error: $error"
                    retryAction = { viewModel.refreshWorkspaces() }
                    // persistent, not just the (dismissable/timed-out) snackbar - otherwise once
                    // that's gone the user is looking at a blank screen with no indication
                    // anything failed, only the toolbar to fall back on
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error ?: "Something went wrong",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Button(
                                onClick = { viewModel.refreshWorkspaces() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // project-group-roles failing doesn't block the workspace list itself (see
        // WorkspaceViewModelImpl.refreshWorkspaces) - just surfaced the same way as the other two
        LaunchedEffect(projectGroupsState) {
            (projectGroupsState as? WorkspaceProjectGroupsState.Error)?.let {
                snackBarMessage = "Error: ${it.error}"
                retryAction = { viewModel.refreshWorkspaces() }
            }
        }

        snackBarMessage?.let {
            LaunchedEffect(snackBarHostState) {
                snackBarHostState.showSnackbar(it, actionLabel = "Refresh").let {
                    if (it == SnackbarResult.ActionPerformed) {
                        retryAction?.invoke()
                    }
                }
                snackBarMessage = null
            }
        }


        if (isLongFormLoading) {
            CircularProgressWithText(text = "Loading workspace details...")
        } else if (projectGroupsState is WorkspaceProjectGroupsState.Loading) {
            // checked before isLoading to match the fetch order in refreshWorkspaces() (project
            // groups first, then workspaces) - isLoading defaults to true before either fetch
            // even starts and doesn't change until the workspaces call begins, so if this were
            // checked second it would never win while project groups is the one actually running
            CircularProgressWithText(text = "Loading project groups...")
        } else if (isLoading) {
            CircularProgressWithText(text = "Loading workspaces...")
        }

        val selectedWorkspaceState by viewModel.selectedWorkspace.collectAsState()
        // bumped by the long-form error's retry action to force LaunchedEffect to re-run without
        // needing selectedWorkspaceState's identity to change
        var longFormRetryTrigger by remember { mutableIntStateOf(0) }
        LaunchedEffect(selectedWorkspaceState, longFormRetryTrigger) {
            selectedWorkspaceState?.let { workspace ->
                viewModel.getWorkspaceDetails(workspace.id).collect { longFormState ->
                    when (longFormState) {
                        is WorkspaceLongFormState.Loading -> {
                            // Handle loading state
                            isLongFormLoading = true
                            snackBarMessage = null
                        }

                        is WorkspaceLongFormState.Success -> {
                            isLongFormLoading = false
                            viewModel.setIsLongForm(true)
                            snackBarMessage = null
//                            settingsViewModel.deleteMapQuests()
                            finishAndLaunchNewActivity(
                                context,
                                longFormState.longFormItems,
                                longFormState.imageryList,
                                workspace,
                                longFormState.featurePresets,
                                longFormState.customIcons,
                                longFormState.recencyPeriodInDays
                            )
                        }

                        is WorkspaceLongFormState.Error -> {
                            isLongFormLoading = false
                            // Handle error state
                            //Show snack bar
                            snackBarMessage = "Error: ${longFormState.error}"
                            retryAction = { longFormRetryTrigger++ }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

fun finishAndLaunchNewActivity(
    context: Context,
    addLongFormResponseItems: List<Elements>,
    imageryList: List<Imagery>?,
    workspace: Workspace,
    featurePresets: List<FeaturePreset> = emptyList(),
    customIcons: List<CustomIcon> = emptyList(),
    recencyPeriodInDays : Int
) {
    val activity = context as? Activity
    activity?.let {
        val intent = Intent(it, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putParcelableArrayListExtra("LONG_FORM", ArrayList(addLongFormResponseItems))
            putExtra("WORKSPACE_TITLE", workspace.title)
            putParcelableArrayListExtra("IMAGERY_LIST", ArrayList(imageryList ?: emptyList()))
            putParcelableArrayListExtra("FEATURE_PRESETS", ArrayList(featurePresets))
            putParcelableArrayListExtra("CUSTOM_ICONS", ArrayList(customIcons))
            putExtra("RECENCY_PERIOD_IN_DAYS", recencyPeriodInDays)
        }
        it.startActivity(intent)
        it.finish()
    }
}

@Composable
fun WorkspaceToolbar(
    onProfileClick: () -> Unit,
    userName: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }
    // nothing to search (empty/loading/error state) - collapse back to the plain wordmark
    LaunchedEffect(showSearch) {
        if (!showSearch) expanded = false
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 20.dp)
        ) {
            UserInitialsAvatar(
                name = userName,
                size = 48.dp,
                modifier = Modifier
                    .clickable { onProfileClick() }
                    .semantics { contentDescription = "Navigate to profile screen" }
            )

            if (showSearch && expanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search workspaces",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.titleMedium,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp)
                        .focusRequester(focusRequester)
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .clearAndSetSemantics {},
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "AVIV",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ProximaNovaFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignBy(LastBaseline)
                    )
                    Text(
                        text = " ScoutRoute",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ProximaNovaFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignBy(LastBaseline)
                    )
                }
            }

            if (showSearch) {
                IconButton(
                    onClick = {
                        if (expanded) {
                            expanded = false
                            onSearchQueryChange("")
                        } else {
                            expanded = true
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.Close else Icons.Default.Search,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = if (expanded) "Close search" else "Search workspaces",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceList(
    onClick: (workspace: Workspace) -> Unit,
    modifier: Modifier = Modifier,
    items: List<Workspace> = emptyList(),
    viewModel: WorkspaceViewModel? = null,
    projectGroups: List<String> = emptyList(),
    projectGroupNames: Map<String, String> = emptyMap(),
    selectedProjectGroup: String? = null,
    onProjectGroupSelected: (String?) -> Unit = {},
) {
    val refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            viewModel?.refreshWorkspaces()
        },
        state = pullRefreshState,
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Please select a workspace to continue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(8.dp)
            )

            if (projectGroups.isNotEmpty()) {
                ProjectGroupFilter(
                    projectGroups = projectGroups,
                    projectGroupNames = projectGroupNames,
                    selectedProjectGroup = selectedProjectGroup,
                    onProjectGroupSelected = onProjectGroupSelected,
                    modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (items.isEmpty()) {
                Text(
                    text = "Nothing found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(modifier = modifier) {
                    items(items = items, key = { it.id }) { workspace ->
                        WorkSpaceListItem(
                            workspace = workspace,
                            modifier = Modifier,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectGroupFilter(
    projectGroups: List<String>,
    selectedProjectGroup: String?,
    onProjectGroupSelected: (String?) -> Unit,
    projectGroupNames: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Filter by Project Group",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            // custom box instead of OutlinedTextField: the latter has no way to ellipsize its
            // (read-only) value text in the middle, only via Text's own overflow param
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .clip(OutlinedTextFieldDefaults.shape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        OutlinedTextFieldDefaults.shape
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = selectedProjectGroup?.let { projectGroupNames[it] ?: it } ?: "All",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All") },
                    onClick = {
                        onProjectGroupSelected(null)
                        expanded = false
                    }
                )
                projectGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(projectGroupNames[group] ?: group) },
                        onClick = {
                            onProjectGroupSelected(group)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkSpaceListItem(
    workspace: Workspace,
    modifier: Modifier = Modifier,
    onClick: (workspace: Workspace) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clickable { onClick(workspace) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workspace.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            Text(
                text = "Created ${formatWorkspaceCreatedAt(workspace.createdAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatWorkspaceCreatedAt(rawCreatedAt: String?): String {
    if (rawCreatedAt.isNullOrBlank()) return "date unknown"
    return try {
        java.time.OffsetDateTime.parse(rawCreatedAt)
            .format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "MMM d, yyyy",
                    java.util.Locale.getDefault()
                )
            )
    } catch (e: Exception) {
        try {
            java.time.Instant.parse(rawCreatedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "MMM d, yyyy",
                        java.util.Locale.getDefault()
                    )
                )
        } catch (e: Exception) {
            rawCreatedAt
        }
    }
}

@Composable
fun CircularProgressWithText(
    text: String,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun WorkSpaceListPreview() {
    WorkspaceList(
        onClick = {},
        modifier = Modifier.fillMaxSize(),
        items = List(10) { Workspace(it, listOf(), "Workspace $it", "osw", externalAppAccess = 1) }
    )
}
