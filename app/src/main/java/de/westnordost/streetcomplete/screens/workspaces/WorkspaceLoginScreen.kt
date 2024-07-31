package de.westnordost.streetcomplete.screens.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ui.theme.AppTheme

@Composable
fun LoginScreen(viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {

}

// fun startLogin(viewModel: WorkspaceViewModel, username: String, password : String){
//     val workspaceListState by viewModel.showWorkspaces.collectAsState()
//
//     when(workspaceListState){
//         is WorkspaceListState.Loading -> {
//             // Show a loading indicator
//             CircularProgressIndicator(modifier)
//         }
//         is WorkspaceListState.Success -> {
//
//             // Display the list of workspaces
//             WorkspaceList(modifier = modifier, items = (workspaceListState as WorkspaceListState.Success).workspaces)
//         }
//         is WorkspaceListState.Error -> {
//             // Show an error message
//             Text("Error: ${(workspaceListState as WorkspaceListState.Error).error}")
//         }
//     }
// }

@Composable
fun LoginCard(modifier: Modifier = Modifier) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(24.dp)
        ) {
            Text(text = "Welcome!", style = MaterialTheme.typography.titleLarge)
            Text(text = "Please Login to your account!", style = MaterialTheme.typography.bodyMedium)
            TextField(
                value = "", onValueChange = {},
                modifier = Modifier.padding(vertical = 16.dp),
                label = {
                    Text(text = stringResource(id = R.string.email))
                }
            )
            TextField(value = "",
                onValueChange = {},
                modifier = Modifier.padding(vertical = 16.dp),
                label = {
                    Text(text = stringResource(id = R.string.password))
                }
            )

            Button(onClick = { }, modifier = Modifier.padding(vertical = 24.dp)) {
                Text(text = "Sign In")
            }
        }
    }
}

@Preview
@Composable
private fun WorkSpaceLoginPreview() {
    AppTheme {
        LoginCard()
    }
}
