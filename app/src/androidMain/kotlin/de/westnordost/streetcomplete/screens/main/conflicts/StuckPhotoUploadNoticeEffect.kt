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
import de.westnordost.streetcomplete.data.osm.edits.create_feature.StuckPhotoUploadNotice
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import kotlinx.coroutines.launch

/**
 * Shows notices about an edit's photo(s) repeatedly failing to upload to KartaView, one at a
 * time - unlike [DiscardedEditNoticeEffect], there IS something to decide here: KartaView is a
 * third-party service outside our control, so the user can choose to keep waiting for it to
 * recover, or drop the photo and submit the rest of their answer without it.
 *
 * Driven by [stuckPhotoUploadNoticesCount] rather than a one-shot event, same as
 * [DiscardedEditNoticeEffect], so it naturally re-triggers and shows the next queued notice, one
 * dialog at a time.
 */
@Composable
fun StuckPhotoUploadNoticeEffect(
    stuckPhotoUploadNoticesCount: Int,
    onPopNextStuckPhotoUploadNotice: suspend () -> StuckPhotoUploadNotice?,
    onRemoveStuckPhoto: suspend (StuckPhotoUploadNotice) -> Unit,
    onKeepTryingStuckPhoto: suspend (StuckPhotoUploadNotice) -> Unit,
    onGetElementLabel: suspend (ElementType, Long) -> String?,
) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<StuckPhotoUploadNotice?>(null) }
    var elementLabel by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }

    LaunchedEffect(stuckPhotoUploadNoticesCount, current) {
        if (current == null && stuckPhotoUploadNoticesCount > 0) {
            val notice = onPopNextStuckPhotoUploadNotice()
            elementLabel = if (notice?.elementType != null && notice.elementId != null) {
                onGetElementLabel(notice.elementType, notice.elementId)
            } else null
            current = notice
        }
    }

    val notice = current ?: return

    fun resolve(action: suspend (StuckPhotoUploadNotice) -> Unit) {
        if (isResolving) return
        isResolving = true
        scope.launch {
            action(notice)
            isResolving = false
            current = null
        }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Photo upload failed") },
        text = {
            val where = elementLabel ?: "near here"
            Text(
                "We couldn't upload the photo for $where after several attempts. You can keep " +
                "trying, or remove the photo and submit the rest of your answer without it."
            )
        },
        confirmButton = {
            TextButton(enabled = !isResolving, onClick = { resolve(onRemoveStuckPhoto) }) {
                Text("Remove photo")
            }
        },
        dismissButton = {
            TextButton(enabled = !isResolving, onClick = { resolve(onKeepTryingStuckPhoto) }) {
                Text("Keep trying")
            }
        }
    )
}
