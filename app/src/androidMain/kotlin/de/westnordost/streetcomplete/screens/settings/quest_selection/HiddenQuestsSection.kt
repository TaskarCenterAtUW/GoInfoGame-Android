package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.quest.OsmNoteQuestKey
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.restore_confirmation
import de.westnordost.streetcomplete.resources.restore_dialog_message
import de.westnordost.streetcomplete.ui.common.dialogs.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource

/** Shows the list of individually hidden quest instances, with the option to unhide each one
 *  (by swiping it away) or all of them at once. Rendered below the quest type selection list */
@Composable
fun HiddenQuestsSection(
    items: List<HiddenQuest>,
    onUnhide: (QuestKey) -> Unit,
    onUnhideAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnhideAllConfirmation by remember { mutableStateOf(false) }

    Column(modifier) {
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "HIDDEN ELEMENTS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { showUnhideAllConfirmation = true },
                enabled = items.isNotEmpty(),
            ) {
                Text("Unhide All")
            }
        }
        Text(
            text = "Swipe left on an item to unhide it and remove it from this list.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No hidden elements",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            items(items, key = { it.key.listKey() }) { item ->
                HiddenQuestRow(
                    item = item,
                    onUnhide = { onUnhide(item.key) },
                )
            }
        }
    }

    if (showUnhideAllConfirmation) {
        ConfirmationDialog(
            onDismissRequest = { showUnhideAllConfirmation = false },
            onConfirmed = onUnhideAll,
            title = { Text(stringResource(Res.string.restore_dialog_message)) },
            confirmButtonText = stringResource(Res.string.restore_confirmation)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenQuestRow(item: HiddenQuest, onUnhide: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onUnhide()
            true
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Unhide",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.idLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Divider()
        }
    }
}

private val HiddenQuest.title: String
    get() = (questType as? AddGenericLong)?.item?.elementType ?: "Create Note"

private val HiddenQuest.idLabel: String
    get() = when (val key = key) {
        is OsmQuestKey -> "ID: ${key.elementId}"
        is OsmNoteQuestKey -> "ID: ${key.noteId}"
    }

private fun QuestKey.listKey(): String = when (this) {
    is OsmQuestKey -> "osm_${elementType}_${elementId}_${questTypeName}_$workspaceId"
    is OsmNoteQuestKey -> "note_$noteId"
}
