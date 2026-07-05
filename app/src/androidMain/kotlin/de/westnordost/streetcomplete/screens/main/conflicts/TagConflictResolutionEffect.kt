package de.westnordost.streetcomplete.screens.main.conflicts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import kotlinx.coroutines.launch

/**
 * Shows currently pending tag conflicts one at a time: while answering a quest, a concurrent
 * remote edit changed the same OSM tag on the same element. Rather than silently discarding the
 * whole answer (the old behavior), the non-conflicting part of the answer already went through -
 * this only asks about the specific tag(s) that collided.
 *
 * Driven by [pendingConflictsCount] rather than a one-shot event so it naturally re-triggers
 * (fetching the next queued conflict) whenever the previous one is resolved, giving a strictly
 * one-dialog-at-a-time queue even if several conflicts piled up while the app was backgrounded.
 */
@Composable
fun TagConflictResolutionEffect(
    pendingConflictsCount: Int,
    onPopNextConflict: suspend () -> PendingTagConflict?,
    onResolveKeepMine: suspend (PendingTagConflict) -> Unit,
    onResolveKeepTheirs: suspend (PendingTagConflict) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<PendingTagConflict?>(null) }
    var isResolving by remember { mutableStateOf(false) }

    LaunchedEffect(pendingConflictsCount, current) {
        if (current == null && pendingConflictsCount > 0) {
            current = onPopNextConflict()
        }
    }

    val conflict = current ?: return

    fun resolve(action: suspend (PendingTagConflict) -> Unit) {
        if (isResolving) return
        isResolving = true
        scope.launch {
            action(conflict)
            isResolving = false
            current = null
        }
    }

    AlertDialog(
        // must make an explicit choice - this is not just informational, it decides what ends up
        // on the server
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Someone else edited this too") },
        text = {
            val mine = conflict.mineValue ?: "(removed)"
            val theirs = conflict.theirsValueAtDetection ?: "(removed)"
            Text(
                "You answered “${conflict.tagKey} = $mine”, but someone else just set " +
                "“${conflict.tagKey} = $theirs” on the same ${conflict.elementType.name.lowercase()} " +
                "while you were answering. Keep your answer?"
            )
        },
        confirmButton = {
            TextButton(enabled = !isResolving, onClick = { resolve(onResolveKeepMine) }) {
                Text("Keep mine")
            }
        },
        dismissButton = {
            TextButton(enabled = !isResolving, onClick = { resolve(onResolveKeepTheirs) }) {
                Text("Keep theirs")
            }
        }
    )
}
