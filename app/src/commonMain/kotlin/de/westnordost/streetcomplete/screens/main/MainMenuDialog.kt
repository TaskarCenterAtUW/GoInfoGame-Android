package de.westnordost.streetcomplete.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.action_download
import de.westnordost.streetcomplete.resources.action_settings
import de.westnordost.streetcomplete.resources.action_switch_workspace
import de.westnordost.streetcomplete.resources.ic_settings_48
import de.westnordost.streetcomplete.ui.common.CloseIcon
import de.westnordost.streetcomplete.ui.common.DownloadIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainMenuDialog(
    onDismissRequest: () -> Unit,
    onClickSettings: () -> Unit,
    onClickDownload: () -> Unit,
    onSwitchWorkspace: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            contentColor = contentColor
        ) {
            Column {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(8.dp).align(Alignment.End)
                ) { CloseIcon() }
                CompactMenuButton(
                    onClick = { onDismissRequest(); onClickSettings() },
                    icon = { },
                    text = stringResource(Res.string.action_settings),
                    )
                CompactMenuButton(
                    onClick = { onDismissRequest(); onClickDownload() },
                    icon = {  },
                    text = stringResource(Res.string.action_download),
                )
                CompactMenuButton(
                    onClick = { onDismissRequest(); onSwitchWorkspace() },
                    icon = { },
                    text = stringResource(Res.string.action_switch_workspace),
                )
                // if (unsyncedEditsCount != null) {
                //     CompactMenuButton(
                //         onClick = { onDismissRequest(); onClickUpload() },
                //         icon = {
                //             UploadIcon()
                //             if (unsyncedEditsCount > 0) {
                //                 NotificationBox {
                //                     Text(
                //                         unsyncedEditsCount.toString(),
                //                         textAlign = TextAlign.Center
                //                     )
                //                 }
                //             }
                //         },
                //         text = stringResource(Res.string.action_upload),
                //         enabled = !isUploadingOrDownloading,
                //     )
                // }
            }
        }
    }
}

@Composable
private fun BigMenuButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactMenuButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surface,
    ),
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMainMenuDialog() {
    MainMenuDialog(
        onDismissRequest = {},
        onClickSettings = {},
        onClickDownload = {},
        onSwitchWorkspace = {}
    )
}
