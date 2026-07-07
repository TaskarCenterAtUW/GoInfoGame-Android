package de.westnordost.streetcomplete.screens.main.conflicts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import kotlinx.coroutines.launch

/**
 * Shows all pending tag conflicts *for one element* together, in a single dialog, instead of one
 * dialog per conflicting tag: while answering a quest, a concurrent remote edit changed some of
 * the same OSM tags on the same element. Rather than silently discarding the whole answer (the
 * old behavior), the non-conflicting part already went through - this only asks about the
 * specific tag(s) that collided, and lets the user decide per tag whether to keep their own answer
 * (checked, the default) or accept the other edit's value (unchecked).
 *
 * Driven by [pendingConflictsCount] rather than a one-shot event so it naturally re-triggers
 * (fetching the next element's group of conflicts) whenever the current one is applied, giving a
 * strictly one-dialog-at-a-time queue even if several elements' conflicts piled up while the app
 * was backgrounded.
 */
@Composable
fun TagConflictResolutionEffect(
    pendingConflictsCount: Int,
    onPopNextConflictGroup: suspend () -> List<PendingTagConflict>,
    onResolveKeepMine: suspend (PendingTagConflict) -> Unit,
    onResolveKeepTheirs: suspend (PendingTagConflict) -> Unit,
    onGetElementLabel: suspend (ElementType, Long) -> String?,
) {
    val scope = rememberCoroutineScope()
    var currentGroup by remember { mutableStateOf<List<PendingTagConflict>>(emptyList()) }
    var elementLabel by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    val keepMine = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(pendingConflictsCount, currentGroup) {
        if (currentGroup.isEmpty() && pendingConflictsCount > 0) {
            val group = onPopNextConflictGroup()
            keepMine.clear()
            // default to keeping the user's own answer - they answered these, assume they still
            // want them unless they uncheck one
            group.forEach { keepMine[it.id] = true }
            elementLabel = group.firstOrNull()?.let { onGetElementLabel(it.elementType, it.elementId) }
            currentGroup = group
        }
    }

    val group = currentGroup
    if (group.isEmpty()) return

    fun apply() {
        if (isApplying) return
        isApplying = true
        scope.launch {
            for (conflict in group) {
                if (keepMine[conflict.id] == true) onResolveKeepMine(conflict) else onResolveKeepTheirs(conflict)
            }
            isApplying = false
            currentGroup = emptyList()
        }
    }

    AlertDialog(
        // must make an explicit choice - this is not just informational, it decides what ends up
        // on the server
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Someone else edited this too") },
        text = {
            Column {
                val where = elementLabel ?: "${group.first().elementType.name.lowercase()} #${group.first().elementId}"
                Text(
                    "While you were answering, someone else changed the following on $where. " +
                    "Keep your answer for each?"
                )
                group.forEach { conflict ->
                    val mine = conflict.mineValue ?: "(removed)"
                    val theirs = conflict.theirsValueAtDetection ?: "(removed)"
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            enabled = !isApplying,
                            checked = keepMine[conflict.id] ?: true,
                            onCheckedChange = { keepMine[conflict.id] = it }
                        )
                        Text("${conflict.tagKey}: you said “$mine”, they set “$theirs”")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isApplying, onClick = ::apply) {
                Text("Apply")
            }
        }
    )
}
