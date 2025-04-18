package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.AddLongFormResponseItem
import de.westnordost.streetcomplete.screens.MainActivity

@Composable
fun WorkSpaceListScreen(viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    val workspaceListState by viewModel.showWorkspaces.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var isLongFormLoading by remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    var snackBarMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val onClick: (index: Int) -> Unit = { index ->
        viewModel.setSelectedWorkspace(index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (workspaceListState) {
            is WorkspaceListState.Loading -> {
                // Show a loading indicator in the center of screen
                isLoading = true
            }

            is WorkspaceListState.Success -> {
                isLoading = false
                // Display the list of workspaces
                val workspaces = (workspaceListState as WorkspaceListState.Success).workspaces
                if (workspaces.isEmpty()) {
                    Text(
                        text = "No workspaces available in your area",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column {
                        Text(
                            text = "Please select a workspace to continue",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = modifier.padding(8.dp)
                        )
                        WorkspaceList(
                            onClick,
                            modifier = Modifier,
                            items = (workspaceListState as WorkspaceListState.Success).workspaces.filter
                            { it.externalAppAccess == 1 && it.type == "osw" },
                            viewModel
                        )
                    }

                }
            }

            is WorkspaceListState.Error -> {
                isLoading = false
                // Show an error message
                snackBarMessage = "Error: ${(workspaceListState as WorkspaceListState.Error).error}"
            }
        }

        snackBarMessage?.let {
            LaunchedEffect(snackBarHostState) {
                snackBarHostState.showSnackbar(it, actionLabel = "Refresh").let {
                    if (it == SnackbarResult.ActionPerformed) {
                        // Retry the action that caused the error
                        viewModel.refreshWorkspaces()
                    }
                }
                snackBarMessage = null
            }
        }


        if (isLoading || isLongFormLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        val selectedWorkspaceState by viewModel.selectedWorkspace.collectAsState()
        LaunchedEffect(selectedWorkspaceState) {
            selectedWorkspaceState?.let { workspaceId ->
                viewModel.getLongForm(workspaceId).collect { longFormState ->
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
                            finishAndLaunchNewActivity(context, longFormState.longFormItems)
                        }

                        is WorkspaceLongFormState.Error -> {
                            isLongFormLoading = false
                            // Handle error state
                            //Show snack bar
                            snackBarMessage = "Error: ${longFormState.error}"
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
    addLongFormResponseItems: List<AddLongFormResponseItem>,
) {
    val activity = context as? Activity
    activity?.let {
        val intent = Intent(it, MainActivity::class.java)
        intent.putParcelableArrayListExtra("LONG_FORM", ArrayList(addLongFormResponseItems))
        it.startActivity(intent)
        it.finish()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WorkspaceList(
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<Workspace> = emptyList(),
    viewModel: WorkspaceViewModel? = null,
) {
    val refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            viewModel?.refreshWorkspaces()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState) // 👈 use the modifier here
    ) {
        LazyColumn(modifier = modifier) {
            itemsIndexed(items) { index, workspace ->
                WorkSpaceListItem(workspace = workspace, workspace.id, Modifier, onClick)
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun WorkSpaceListItem(
    workspace: Workspace,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (index: Int) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick(index) }
    ) {
        Text(
            text = workspace.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
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
