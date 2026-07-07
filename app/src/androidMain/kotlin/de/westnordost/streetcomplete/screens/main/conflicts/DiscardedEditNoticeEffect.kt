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
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNotice
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import kotlinx.coroutines.launch

/**
 * Shows notices about edits that had to be discarded one at a time: the underlying element was
 * deleted or changed too substantially in the meantime for the answer to still apply. Unlike
 * [TagConflictResolutionEffect], there is nothing to decide here - the quest already disappeared
 * from the map when it was answered, and without this, the user would only find out something
 * went wrong much later when the quest silently reappears with no explanation.
 *
 * Driven by [discardedNoticesCount] rather than a one-shot event, same as [TagConflictResolutionEffect],
 * so it naturally re-triggers and shows the next queued notice, one dialog at a time.
 */
@Composable
fun DiscardedEditNoticeEffect(
    discardedNoticesCount: Int,
    onPopNextDiscardedNotice: suspend () -> DiscardedEditNotice?,
    onDismissDiscardedNotice: suspend (DiscardedEditNotice) -> Unit,
    onGetElementLabel: suspend (ElementType, Long) -> String?,
) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<DiscardedEditNotice?>(null) }
    var elementLabel by remember { mutableStateOf<String?>(null) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(discardedNoticesCount, current) {
        if (current == null && discardedNoticesCount > 0) {
            val notice = onPopNextDiscardedNotice()
            elementLabel = if (notice?.elementType != null && notice.elementId != null) {
                onGetElementLabel(notice.elementType, notice.elementId)
            } else null
            current = notice
        }
    }

    val notice = current ?: return

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            onDismissDiscardedNotice(notice)
            isDismissing = false
            current = null
        }
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text("Answer could not be saved") },
        text = {
            val where = elementLabel ?: "near here"
            Text(
                "Your answer for ${notice.editType.name} on $where could not be saved \n Reason: " +
                "${notice.reason}."
            )
        },
        confirmButton = {
            TextButton(enabled = !isDismissing, onClick = ::dismiss) {
                Text("OK")
            }
        }
    )
}
