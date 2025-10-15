package de.westnordost.streetcomplete.screens.workspaces

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import de.westnordost.streetcomplete.BuildConfig
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.workspace.data.remote.Environment
import de.westnordost.streetcomplete.data.workspace.data.remote.EnvironmentManager
import de.westnordost.streetcomplete.ui.theme.ProximaNovaFontFamily
import de.westnordost.streetcomplete.util.creds_manager.BiometricHelper
import de.westnordost.streetcomplete.util.creds_manager.EnvCredentials
import de.westnordost.streetcomplete.util.creds_manager.SecureCredentialStorage
import de.westnordost.streetcomplete.util.location.FineLocationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.java.KoinJavaComponent.getKoin

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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferences.isBiometricEnabled && !state.expediteLogin) {
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
        LoginCard(viewModel, email, password, selectedEnvironment, activity, preferences, modifier)

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        snackBarMessage?.let {
            LaunchedEffect(snackBarHostState) {
                snackBarHostState.showSnackbar(it, duration = SnackbarDuration.Indefinite, withDismissAction = true)
            }
        }
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    LaunchedEffect(activity) {
        checkForIntent(activity, viewModel, environmentManager, selectedEnvironment, preferences)
    }
}

fun checkForIntent(
    activity: AppCompatActivity,
    viewModel: WorkspaceViewModel,
    environmentManager: EnvironmentManager,
    selectedEnvironment: MutableState<Environment>,
    preferences: Preferences
) {
    val data: Uri? = activity.intent?.data
    data?.let {
        val refreshToken = it.getQueryParameter("code") // e.g. ?code=123
        val env = it.getQueryParameter("env") // e.g. ?env=staging
        preferences.workspaceRefreshToken = refreshToken
        if (!preferences.workspaceLogin) {
            if (env != null) {
                try {
                    val environment = Environment.valueOf(env.uppercase())
                    environmentManager.currentEnvironment = environment
                    selectedEnvironment.value = environment
                } catch (e: IllegalArgumentException) {
                    // Invalid environment value, handle as needed
                    Toast.makeText(
                        activity.baseContext,
                        "Invalid environment value in the link. " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            viewModel.refreshToken(true)
        } else {
            Toast.makeText(
                activity.baseContext,
                "User already logged in. Please logout to sign in using new credentials",
                Toast.LENGTH_SHORT
            ).show()
        }

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
            title = { Text(stringResource(R.string.save_credentials)) },
            text = { Text(stringResource(R.string.save_credentials_message)) },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val authenticated = authenticateWithBiometrics(
                            context,
                            activity = activity
                        )
                        if (!authenticated) {
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
                Button(onClick = {
                    openDialog.value = false
                    navToNextPage()
                }) {
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
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(verticalArrangement = Arrangement.SpaceEvenly) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .background(Color(0xFFE7E3EE))
            ) {
                Row(modifier = Modifier.padding(all = 32.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.aviv_logo),
                        contentDescription = "Workspace Icon",
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .padding(end = 16.dp)
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = "AVIV",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ProximaNovaFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "ScoutRoute",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ProximaNovaFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                val context = LocalContext.current
                var visibility by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    value = email.value, onValueChange = { newText -> email.value = newText },
                    label = {
                        Text(
                            text = stringResource(
                                id = R.string.email
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                )
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { newText -> password.value = newText },
                    label = {
                        Text(
                            text = stringResource(
                                id = R.string.password
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    visualTransformation = if (visibility) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        val image =
                            if (visibility) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { visibility = !visibility }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            if (email.value.isEmpty() || password.value.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please enter email and password",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            viewModel.loginToWorkspace(email.value, password.value)
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "Login", style = MaterialTheme.typography.titleMedium)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferences.isBiometricEnabled) {
                        val coroutineScope = rememberCoroutineScope()
                        val creds = SecureCredentialStorage.getCredential(
                            LocalContext.current,
                            selectedEnvironment.value.name
                        )
                        if (creds != null) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val authenticated = authenticateWithBiometrics(
                                            context,
                                            activity = activity
                                        )
                                        if (!authenticated) {
                                            Toast.makeText(
                                                context,
                                                "Failed to authenticate",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        } else {
                                            email.value = creds.username
                                            password.value = creds.password
                                            viewModel.loginToWorkspace(email.value, password.value)
                                        }
                                    }

                                }, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = androidx.biometric.R.drawable.fingerprint_dialog_fp_icon),
                                    contentDescription = "Fingerprint Icon",
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(
                                    text = "Login with Device Authentication",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    if (BuildConfig.SHOW_DEBUG_OPTIONS) {
                        DebuggableBuild(
                            viewModel,
                            selectedEnvironment,
                            preferences,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebuggableBuild(
    viewModel: WorkspaceViewModel,
    selectedEnvironment: MutableState<Environment>,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    val isDebugModeEnabled by preferences.isDebugModeEnabled.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isDebugModeEnabled) {
                EnvironmentDropdownMenu(viewModel, selectedEnvironment, modifier = Modifier)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Exit debug mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            showDialog = true
                        },
                )
            } else {
                viewModel.setEnvironment(Environment.PROD)
            }

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        clickCount++
                        if (clickCount == 7) {
                            showDialog = true
                        }
                    }
            )
        }

        if (showDialog) {
            ShowDebugModeConfirmationDialog(
                enable = !isDebugModeEnabled,
                onConfirm = {
                    preferences.setDebugModeEnabled(!isDebugModeEnabled)
                    clickCount = 0
                    showDialog = false
                    selectedEnvironment.value = Environment.PROD
                },
                onCancel = {
                    clickCount = 0
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ShowDebugModeConfirmationDialog(enable: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val openDialog = remember { mutableStateOf(true) }
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (enable) "Enable Debug Mode" else "Disable Debug Mode") },
            text = { Text(if (enable) "Are you sure you want to enable debug mode?" else "Are you sure you want to disable debug mode?") },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                    onConfirm()
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { onCancel() }) {
                    Text("Cancel")
                }
            }
        )
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)

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


