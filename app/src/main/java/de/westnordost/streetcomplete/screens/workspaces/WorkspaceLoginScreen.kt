package de.westnordost.streetcomplete.screens.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import de.westnordost.osmapi.OsmConnection
import de.westnordost.streetcomplete.R
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject

@Composable
fun LoginScreen(
    viewModel: WorkspaceViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    val navToNextPage = {
        navController.navigate("workspace-list")
    }
    val loginState by viewModel.loginState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val snackBarHostState = remember{ SnackbarHostState() }
    var snackBarMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (loginState) {
            is WorkspaceLoginState.Init -> {
                isLoading = false
            }
            is WorkspaceLoginState.Loading -> {
                isLoading = true
            }

            is WorkspaceLoginState.Error -> {
                isLoading = false
                Text(text = "Error: ${(loginState as WorkspaceLoginState.Error).error}")
                snackBarMessage = (loginState as WorkspaceLoginState.Error).error
            }

            is WorkspaceLoginState.Success -> {
                isLoading = false
                val state= loginState as WorkspaceLoginState.Success
                viewModel.setLoginState(true, state.loginResponse)
                navToNextPage()
                Text(text = "Success: ${(loginState as WorkspaceLoginState.Success).loginResponse}")
            }
        }
        LoginCard(viewModel, modifier)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        snackBarMessage?.let {
            LaunchedEffect(snackBarHostState) {
                snackBarHostState.showSnackbar(it)
            }
        }
        SnackbarHost(hostState = snackBarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun LoginCard(viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(24.dp)
        ) {

            Text(text = "Welcome!", style = MaterialTheme.typography.titleLarge)
            Text(text = "Please Login to your account!", style = MaterialTheme.typography.bodyMedium)
            TextField(
                value = email, onValueChange = {newText -> email = newText},
                modifier = Modifier.padding(vertical = 16.dp),
                label = {
                    Text(text = stringResource(id = R.string.email))
                }
            )
            TextField(value = password,
                onValueChange = {newText -> password = newText},
                label = {
                    Text(text = stringResource(id = R.string.password))
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(vertical = 16.dp),
                )

            Button(onClick = {
                viewModel.loginToWorkspace(email, password)
            }, modifier = Modifier.padding(vertical = 24.dp)) {
                Text(text = "Sign In")
            }
        }
    }
}

