package de.westnordost.streetcomplete.screens.workspaces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace

@Composable
fun WorkSpaceListScreen(viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    val workspaceListState by viewModel.showWorkspaces.collectAsState()

    when(workspaceListState){
        is WorkspaceListState.Loading -> {
            // Show a loading indicator
            CircularProgressIndicator(modifier)
        }
        is WorkspaceListState.Success -> {

            // Display the list of workspaces
            WorkspaceList(modifier = modifier, items = (workspaceListState as WorkspaceListState.Success).workspaces)
        }
        is WorkspaceListState.Error -> {
            // Show an error message
            Text("Error: ${(workspaceListState as WorkspaceListState.Error).error}")
        }
    }
}

@Composable
fun WorkspaceList(modifier : Modifier = Modifier, items: List<Workspace> = emptyList()) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items) { index, workspace ->
            WorkSpaceListItem(workspace = workspace)
        }
    }
}

@Composable
fun WorkSpaceListItem(workspace: Workspace, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
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
        modifier = Modifier.fillMaxSize(),
        items = List(10) { Workspace(it, listOf(), "Workspace $it") })
}
