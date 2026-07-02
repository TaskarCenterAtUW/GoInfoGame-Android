package de.westnordost.streetcomplete.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = contentColor
        ) {
            Column {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(8.dp).align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = "Close"
                    )
                }
                Card(modifier = Modifier.padding(8.dp)) {
                    CompactMenuButton(
                        onClick = { onDismissRequest(); onClickSettings() },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        text = stringResource(Res.string.action_settings),
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface) // Force background to match buttons
                            .padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                    CompactMenuButton(
                        onClick = { onDismissRequest(); onClickDownload() },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null
                            )
                        },
                        text = "Fetch map data",
                        description = "Fetch the latest quests from the server for this area"
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface) // Force background to match buttons
                            .padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                    CompactMenuButton(
                        onClick = { onDismissRequest(); onSwitchWorkspace() },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = "Download"
                            )
                        },
                        text = stringResource(Res.string.action_switch_workspace),
                    )
                }

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
    description: String = "",
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
            Column {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (description.isNotEmpty()){
                    Text(
                        text = description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
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
