package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.workspace.domain.model.Workspace
import de.westnordost.streetcomplete.screens.MainActivity

@Composable
fun WorkSpaceListScreen(viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    val workspaceListState by viewModel.showWorkspaces.collectAsState()
    val context = LocalContext.current

    val onClick : (index : Int) -> Unit = { index ->
        when (index) {
            0 -> {
                viewModel.setIsLongForm(true)
            }

            else -> {
                viewModel.setIsLongForm(false)
            }
        }
        finishAndLaunchNewActivity(context)
    }

    when(workspaceListState){
        is WorkspaceListState.Loading -> {
            // Show a loading indicator
            CircularProgressIndicator(modifier)
        }
        is WorkspaceListState.Success -> {

            // Display the list of workspaces
            WorkspaceList(onClick, modifier = modifier, items = (workspaceListState as WorkspaceListState.Success).workspaces)
        }
        is WorkspaceListState.Error -> {
            // Show an error message
            Text("Error: ${(workspaceListState as WorkspaceListState.Error).error}")
        }
    }
}

fun finishAndLaunchNewActivity(context: Context) {
    val activity = context as? Activity
    activity?.let {
        it.finish()
        val intent = Intent(it, MainActivity::class.java)
        it.startActivity(intent)
    }
}

@Composable
fun WorkspaceList(
    onClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<Workspace> = emptyList()
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items) { index, workspace ->
            WorkSpaceListItem(workspace = workspace,index, Modifier, onClick)
        }
    }
}

@Composable
fun WorkSpaceListItem(
    workspace: Workspace,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (index: Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = {onClick(index)},
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = workspace.title!!,
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
        items = List(10) { Workspace(it, listOf(), "Workspace $it") })
}
