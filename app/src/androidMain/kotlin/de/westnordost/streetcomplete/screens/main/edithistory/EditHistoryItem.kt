package de.westnordost.streetcomplete.screens.main.edithistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.osm.osmquests.OsmQuestHidden
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.screens.main.controls.MapButton
import de.westnordost.streetcomplete.ui.common.UndoIcon
import de.westnordost.streetcomplete.ui.theme.selectionBackground
import org.jetbrains.compose.ui.tooling.preview.Preview

/** One item in the edit history sidebar list. Selectable and when selected, an undo button is
 *  clickable. */
@Composable
fun EditHistoryItem(
    selected: Boolean,
    onSelect: () -> Unit,
    onUndo: () -> Unit,
    edit: Edit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.selectionBackground
        edit.isSynced == true -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    val name = edit.getName()

    // Triple-tap detection state
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(backgroundColor)

            .pointerInput(selected) { // Handle physical touch
                detectTapGestures {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime <= 500) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime

                    when {
                        selected && tapCount >= 3 -> {
                            tapCount = 0
                            lastTapTime = 0L
                            onUndo()
                        }
                        tapCount == 1 -> {
                            onSelect()
                        }
                    }
                }
            }
            .clearAndSetSemantics { // Isolate parent semantics for accessibility
                contentDescription = if (selected) {
                    "Selected, $name edit. Triple tap to undo this edit"
                } else {
                    "$name edit"
                }

                // Selection action for accessibility users
                onClick(label = if (selected) "deselect" else "select") {
                    onSelect()
                    true
                }

                // Undo action available when selected for accessibility users
                if (selected) {
                    onClick(label = "undo") {
                        onUndo()
                        true
                    }
                }
            }

    ) {
        Box(
            Modifier
                .size(56.dp)
                .padding(4.dp)
                .semantics(mergeDescendants = false) {} // Don't merge to allow MapButton semantics
        ) {
            EditImage(edit)
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MapButton(onClick = onUndo, contentPadding = 8.dp) { UndoIcon() }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewEditsColumnItem() {
    var selected by remember { mutableStateOf(false) }
    EditHistoryItem(
        selected = selected,
        onSelect = { selected = !selected },
        onUndo = {},
        modifier = Modifier.width(80.dp),
        edit = OsmQuestHidden(ElementType.NODE, 1L, AddGenericLong(Elements(), recencyPeriodInDays = 90), ElementPointGeometry(LatLon(0.0, 0.0)), 1L, 0),
    )
}
