package de.westnordost.streetcomplete.screens.settings.overlay_selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.action_reset
import de.westnordost.streetcomplete.resources.pref_overlays_reset
import de.westnordost.streetcomplete.resources.pref_subtitle_quests_preset_name
import de.westnordost.streetcomplete.resources.pref_title_overlays
import de.westnordost.streetcomplete.ui.common.BackIcon
import de.westnordost.streetcomplete.ui.common.DropdownMenuItem
import de.westnordost.streetcomplete.ui.common.MoreIcon
import de.westnordost.streetcomplete.ui.common.dialogs.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource

/** Top bar for the overlay selection screen */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySelectionTopAppBar(
    currentPresetName: String,
    onClickBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { OverlaySelectionTitle(currentPresetName ) },
        windowInsets = TopAppBarDefaults.windowInsets,
        navigationIcon = { IconButton(onClick = onClickBack) { BackIcon() } },
        actions = { OverlaySelectionTopBarActions(onReset = onReset) },
        modifier = modifier,
    )
}

@Composable
private fun OverlaySelectionTitle(currentPresetName: String) {
    Column {
        Text(
            text = stringResource(Res.string.pref_title_overlays),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(Res.string.pref_subtitle_quests_preset_name, currentPresetName),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OverlaySelectionTopBarActions(
    onReset: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showActionsDropdown by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showActionsDropdown = true }) { MoreIcon() }
        DropdownMenu(
            expanded = showActionsDropdown,
            onDismissRequest = { showActionsDropdown = false },
        ) {
            DropdownMenuItem(onClick = {
                showResetDialog = true
                showActionsDropdown = false
            }) {
                Text(stringResource(Res.string.action_reset))
            }
        }
    }

    if (showResetDialog) {
        ConfirmationDialog(
            onDismissRequest = { showResetDialog = false },
            onConfirmed = onReset,
            text = { Text(stringResource(Res.string.pref_overlays_reset)) },
        )
    }
}
