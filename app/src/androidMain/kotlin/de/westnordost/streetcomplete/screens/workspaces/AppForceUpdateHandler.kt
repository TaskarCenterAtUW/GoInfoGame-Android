package de.westnordost.streetcomplete.screens.workspaces

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppForceUpdateHandler(viewModel: WorkspaceViewModel) {
    val appForceUpdateState by viewModel.updateState.collectAsStateWithLifecycle()
    when (appForceUpdateState) {
        is AppVersionUpdateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressWithText("Checking for updates...")
            }
        }

        is AppVersionUpdateState.Success -> {
            if ((appForceUpdateState as AppVersionUpdateState.Success).isForceUpdate) {
                val updateUrl = (appForceUpdateState as AppVersionUpdateState.Success).updateUrl
                ForceUpdateScreen(updateUrl = updateUrl)
            } else if ((appForceUpdateState as AppVersionUpdateState.Success).isUpdateAvailable) {
                val updateUrl = (appForceUpdateState as AppVersionUpdateState.Success).updateUrl
                OptionalUpdateBanner(updateUrl = updateUrl)
            }
        }

        is AppVersionUpdateState.Error -> {
            Toast.makeText(
                LocalContext.current,
                "Error while checking for app update : ${(appForceUpdateState as AppVersionUpdateState.Error).error}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun OptionalUpdateBanner(updateUrl: String) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(true) }

    if (!open) return
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    updateUrl.toUri()
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }) {
                Text(text = "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                open = false
            }) {
                Text(text = "Later")
            }
        },
        title = { Text(text = "Update required") },
        text = {
            Text(
                text = "A new version of the app is available. Would you like to update now?"
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun ForceUpdateScreen(updateUrl: String) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(true) }

    if (!open) return
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    updateUrl.toUri()
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                open = false
            }) {
                Text(text = "Update")
            }
        },
        title = { Text(text = "Update required") },
        text = {
            Text(
                text = "A new version of the app is available. Please update to continue using the app."
            )
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
