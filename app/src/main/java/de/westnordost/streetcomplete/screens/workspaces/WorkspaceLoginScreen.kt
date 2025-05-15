package de.westnordost.streetcomplete.screens.workspaces

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.Environment
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.util.creds_manager.BiometricHelper
import de.westnordost.streetcomplete.util.creds_manager.EnvCredentials
import de.westnordost.streetcomplete.util.creds_manager.SecureCredentialStorage
import de.westnordost.streetcomplete.util.location.FineLocationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@SuppressLint("MissingPermission")
@Composable
fun LoginScreen(
    viewModel: WorkspaceViewModel,
    environmentManager: EnvironmentManager,
    preferences: Preferences,
    navController: NavHostController,
    activity: AppCompatActivity,
    modifier: Modifier = Modifier,
) {
    val loginState by viewModel.loginState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }
    var snackBarMessage by remember { mutableStateOf<String?>(null) }
    val selectedEnvironment = remember { mutableStateOf(environmentManager.currentEnvironment) }

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val context = LocalContext.current
    val navToNextPage = {
        val fineLocationManager = FineLocationManager(context) { location ->
            // Do something with the location
            viewModel.fetchWorkspaces(location)
        }
        fineLocationManager.getCurrentLocation()
        navController.navigate("workspace-list")
    }
    Box(modifier = Modifier.fillMaxSize()) {
        when (loginState) {
            is WorkspaceLoginState.Init -> {
                isLoading = false
                snackBarMessage = null
            }

            is WorkspaceLoginState.Loading -> {
                isLoading = true
                snackBarMessage = null
            }

            is WorkspaceLoginState.Error -> {
                isLoading = false
                snackBarMessage = (loginState as WorkspaceLoginState.Error).error
            }

            is WorkspaceLoginState.Success -> {
                isLoading = false
                snackBarMessage = null
                val state = loginState as WorkspaceLoginState.Success
                viewModel.setLoginState(true, state.loginResponse, state.email)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val creds = SecureCredentialStorage.getCredential(
                        context,
                        selectedEnvironment.value.name
                    )
                    if (creds != null) {
                        if (creds.username == email.value && creds.password == password.value) {
                            navToNextPage()
                        } else {
                            ShowSaveCredsDialog(
                                email.value, password.value,
                                selectedEnvironment.value.name, activity, navToNextPage
                            )
                        }
                    } else {
                        ShowSaveCredsDialog(
                            email.value, password.value,
                            selectedEnvironment.value.name, activity, navToNextPage
                        )
                    }
                } else {
                    navToNextPage()
                }
            }
        }
        LoginCard(viewModel, email, password, selectedEnvironment, activity,preferences, modifier)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        snackBarMessage?.let {
            LaunchedEffect(snackBarHostState) {
                snackBarHostState.showSnackbar(it, duration = SnackbarDuration.Indefinite)
            }
        }
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ShowSaveCredsDialog(
    username: String, password: String, environment: String,
    activity: AppCompatActivity,
    navToNextPage: () -> Unit
) {
    // Example Compose dialog to save credentials
    val openDialog = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text("Save Credentials?") },
            text = { Text("Would you like to save your credentials securely on this device?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val authenticated = authenticateWithBiometrics(
                            context,
                            activity = activity
                        )
                        if (!authenticated){
                            Toast.makeText(context, "Failed to authenticate", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val credsMap = SecureCredentialStorage.loadCredentials(context)
                            credsMap[environment] = EnvCredentials(username, password)
                            SecureCredentialStorage.saveCredentials(
                                context,
                                credsMap
                            )
                            navToNextPage()
                        }
                    }
                    openDialog.value = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { openDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LoginCard(
    viewModel: WorkspaceViewModel,
    email: MutableState<String>,
    password: MutableState<String>,
    selectedEnvironment: MutableState<Environment>,
    activity: AppCompatActivity,
    preferences: Preferences,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(24.dp)
        ) {
            val context = LocalContext.current
            Text(text = "Welcome!", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Please login to your account!",
                style = MaterialTheme.typography.bodyMedium
            )
            TextField(
                value = email.value, onValueChange = { newText -> email.value = newText },
                label = {
                    Text(
                        text = stringResource(
                            id = R.string.email,
                            selectedEnvironment.value.name.lowercase()
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .padding(vertical = 16.dp)
            )
            TextField(
                value = password.value,
                onValueChange = { newText -> password.value = newText },
                label = {
                    Text(
                        text = stringResource(
                            id = R.string.password,
                            selectedEnvironment.value.name.lowercase()
                        )
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .padding(vertical = 16.dp)
            )

            Button(onClick = {
                if (email.value.isEmpty() || password.value.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }
                viewModel.loginToWorkspace(email.value, password.value)
            }, modifier = Modifier.padding(vertical = 24.dp)) {
                Text(text = "Sign In", style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Arrow Icon",
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferences.isBiometricEnabled) {
                val coroutineScope = rememberCoroutineScope()
                val creds = SecureCredentialStorage.getCredential(
                    LocalContext.current,
                    selectedEnvironment.value.name
                )
                if (creds != null) {
                    Button(onClick = {
                        coroutineScope.launch {
                            val authenticated = authenticateWithBiometrics(
                                context,
                                activity = activity
                            )
                            if (!authenticated){
                                Toast.makeText(context, "Failed to authenticate", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                email.value = creds.username
                                password.value = creds.password
                                viewModel.loginToWorkspace(email.value, password.value)
                            }
                        }

                    }, modifier = Modifier.padding(vertical = 24.dp)) {
                        Icon(
                            painter = painterResource(id = androidx.biometric.R.drawable.fingerprint_dialog_fp_icon),
                            contentDescription = "Fingerprint Icon",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(text = "Sign In with Device Authentication", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            EnvironmentDropdownMenu(viewModel = viewModel, selectedEnvironment, modifier)
        }
    }
}

@Composable
fun EnvironmentDropdownMenu(
    viewModel: WorkspaceViewModel,
    selectedEnvironment: MutableState<Environment>,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row {
            Text(
                text = "Select Environment :",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically)
            )
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)

            ) {
                Text(text = selectedEnvironment.value.name)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 300.dp, y = 0.dp)
        ) {
            Environment.entries.forEach { environment ->
                DropdownMenuItem(text = { Text(environment.name) }, onClick = {
                    selectedEnvironment.value = environment
                    expanded = false
                    viewModel.setEnvironment(environment)
                })
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun authenticateWithBiometrics(
    context: Context,
    activity: FragmentActivity
): Boolean = suspendCancellableCoroutine { continuation ->
    val biometricHelper = BiometricHelper(
        context = context,
        activity = activity,
        onSuccess = {
            if (continuation.isActive) {
                continuation.resume(true) {}
            }
        },
        onFailure = {
            if (continuation.isActive) {
                continuation.resume(false) {}
            }
            Toast.makeText(context, "Failed to authenticate", Toast.LENGTH_SHORT).show()
        }
    )

    biometricHelper.authenticate()
}


