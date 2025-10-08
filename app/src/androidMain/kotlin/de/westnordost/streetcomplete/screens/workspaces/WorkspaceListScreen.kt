package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.screens.main.MainActivity
import de.westnordost.streetcomplete.screens.user.UserActivity
import de.westnordost.streetcomplete.ui.theme.ProximaNovaFontFamily
import de.westnordost.streetcomplete.util.satellite_layers.Imagery

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
//        Toast.makeText(context, index.toString() + " " + viewModel.selectedWorkspace.value?.id.toString(), Toast.LENGTH_SHORT).show()
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val context = LocalContext.current
                        Icon(
                            imageVector = Icons.Default.Person,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Star Icon",
                            modifier = Modifier
                                .padding(16.dp)
                                .size(36.dp, 36.dp)
                                .clickable {
                                    val intent = Intent(
                                        context,
                                        UserActivity::class.java
                                    )
                                    context.startActivity(intent)
                                }

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
            CircularProgressWithText(
                text = "Loading workspaces..."
            )
        }

        val selectedWorkspaceState by viewModel.selectedWorkspace.collectAsState()
        LaunchedEffect(selectedWorkspaceState) {
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
                                workspace
                            )
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
    addLongFormResponseItems: List<Elements>,
    imageryList: List<Imagery>?,
    workspace: Workspace
) {
    val activity = context as? Activity
    activity?.let {
        val intent = Intent(it, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putParcelableArrayListExtra("LONG_FORM", ArrayList(addLongFormResponseItems))
            putExtra("WORKSPACE_TITLE", workspace.title)
            putParcelableArrayListExtra("IMAGERY_LIST", ArrayList(imageryList ?: emptyList()))
        }
        it.startActivity(intent)
        it.finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceList(
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<Workspace> = emptyList(),
    viewModel: WorkspaceViewModel? = null,
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
            Image(
                painter = painterResource(id = R.drawable.workspaces_logo_purple),
                contentDescription = "Workspace Icon",
                alignment = Alignment.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(end = 16.dp)
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "AVIV",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ProximaNovaFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alignBy(LastBaseline)
                )
                Text(
                    "ScoutRoute",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ProximaNovaFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alignBy(LastBaseline)
                )
            }

            Text(
                text = "Please select a workspace to continue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(8.dp)
            )
            LazyColumn(modifier = modifier) {
                itemsIndexed(items) { index, workspace ->
                    WorkSpaceListItem(workspace = workspace, index, Modifier, onClick)
                }
            }
        }
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
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick(index) }
    ) {
        Text(
            text = workspace.title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentWidth(align = Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun CircularProgressWithText(
    text: String
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
