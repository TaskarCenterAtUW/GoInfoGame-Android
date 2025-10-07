package de.westnordost.streetcomplete.screens.workspaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PermissionHandler(
    permissions: List<PermissionModel>,
    askPermission: Boolean,
    result: (Map<String, Boolean>) -> Unit = {},
    permissionGranted: () -> Unit = {}
) {
    var permissionGrantedState by remember { mutableStateOf(false) }
    val activity = LocalContext.current as Activity
    val viewModel: PermissionViewModel = viewModel(
        factory = PermissionViewModelFactory(
            permissions = permissions,
            askPermission = askPermission
        )
    )
    val state = viewModel.state.collectAsStateWithLifecycle().value

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            result(it)
            viewModel.onResult(it)
        }
    )
    LaunchedEffect(key1 = state.askPermission, block = {
        if (state.askPermission) {
            permissionLauncher.launch(state.permissions.toTypedArray())
        }
    })
    LaunchedEffect(key1 = state.navigateToSetting, block = {
        if (state.navigateToSetting) {
            activity.openAppSetting()
            viewModel.onPermissionRequested()
        }
    })
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Register the lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check permission when the app resumes
                permissionGrantedState = hasAllPermissions(
                    context,
                    state.permissions.toTypedArray()
                )
                if (permissionGrantedState) {
                    permissionGranted()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup the observer when the composable leaves the screen
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AnimatedVisibility(
        visible = state.showRational && !permissionGrantedState,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Access denied",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            state.rationals.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}) $item",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Button(
                onClick = viewModel::onGrantPermissionClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(text = "Grant Permission", modifier = Modifier.padding(vertical = 4.dp))
            }

        }
    }
}

fun hasAllPermissions(context: Context, permissionsToRequest: Array<String>): Boolean {
    return permissionsToRequest.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

data class PermissionModel(
    val permission: String,
    val maxSDKVersion: Int,
    val minSDKVersion: Int,
    val rational: String,
)

data class PermissionState(
    val askPermission: Boolean,
    val showRational: Boolean = false,
    val rationals: List<String> = emptyList(),
    val permissions: List<String>,
    val navigateToSetting: Boolean = false
)

fun Activity.openAppSetting() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
