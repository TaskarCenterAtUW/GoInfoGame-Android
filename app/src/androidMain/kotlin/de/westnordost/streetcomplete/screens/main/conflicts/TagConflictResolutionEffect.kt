package de.westnordost.streetcomplete.screens.main.conflicts

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflict
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import kotlinx.coroutines.launch

/**
 * Shows all pending tag conflicts *of one held-back edit* together, in a single "Resolve
 * Conflicts" bottom sheet: while answering a quest, a concurrent remote edit changed some of the
 * same OSM tags on the same element. The whole answer is held back from uploading - nothing has
 * been submitted yet. Each conflict is shown as the long-form question it came from (falling back
 * to the raw tag key for non-long-form edits) with two tappable rows - the user's own answer
 * (selected by default) and the value someone else set. Confirm folds the per-row choices into
 * the held edit and it then uploads as one unit through the normal sync path.
 *
 * Cancel (or swiping the sheet away) postpones instead of resolving: the answer stays held (and
 * counted in the toolbar badge as an unsynced edit), and the sheet comes back when the pending
 * count changes (new conflict, sync), when the user taps the toolbar upload button
 * ([reviewRequests] bumps), or on app restart.
 *
 * Confirm dismisses the sheet immediately; the decisions are applied locally in the background
 * and [onResolutionFinished] then triggers the upload, whose progress shows in the toolbar
 * spinner as usual.
 *
 * Driven by [pendingConflictsCount] rather than a one-shot event so it naturally re-triggers
 * (fetching the next edit's group of conflicts) whenever the current one is applied, giving a
 * strictly one-sheet-at-a-time queue even if several edits' conflicts piled up while the app
 * was backgrounded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagConflictResolutionEffect(
    pendingConflictsCount: Int,
    reviewRequests: Int,
    onPopNextConflictGroup: suspend () -> List<PendingTagConflict>,
    onResolveKeepMine: suspend (PendingTagConflict) -> Unit,
    onResolveKeepTheirs: suspend (PendingTagConflict) -> Unit,
    onGetElementLabel: suspend (ElementType, Long) -> String?,
    onResolutionFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var currentGroup by remember { mutableStateOf<List<PendingTagConflict>>(emptyList()) }
    var elementLabel by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    // the count at which the user hit Cancel - suppresses re-fetching until the count changes
    // (i.e. something new happened), the user taps the upload button, or the app restarts
    var postponedAtCount by remember { mutableStateOf<Int?>(null) }
    var lastReviewRequests by remember { mutableStateOf(reviewRequests) }
    val keepMine = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(pendingConflictsCount, currentGroup, reviewRequests, isApplying) {
        if (reviewRequests != lastReviewRequests) {
            lastReviewRequests = reviewRequests
            postponedAtCount = null
        }
        if (postponedAtCount != null && postponedAtCount != pendingConflictsCount) {
            postponedAtCount = null
        }
        // !isApplying: the rows being resolved in the background are still in the DB until each
        // one completes - fetching now would just pop the same group again
        if (currentGroup.isEmpty() && pendingConflictsCount > 0 && postponedAtCount == null && !isApplying) {
            val group = onPopNextConflictGroup()
            keepMine.clear()
            // default to keeping the user's own answer - they answered these, assume they still
            // want them unless they pick the existing value
            group.forEach { keepMine[it.id] = true }
            elementLabel = group.firstOrNull()?.let { onGetElementLabel(it.elementType, it.elementId) }
            currentGroup = group
        }
    }

    val group = currentGroup
    if (group.isEmpty()) return

    fun postpone() {
        postponedAtCount = pendingConflictsCount
        currentGroup = emptyList()
    }

    fun apply() {
        if (isApplying) return
        isApplying = true
        val choices = group.map { it to (keepMine[it.id] ?: true) }
        // dismiss right away - resolution continues in the background, surfaced via the toolbar
        // spinner, and rows only leave the DB/badge as they actually resolve
        currentGroup = emptyList()
        scope.launch {
            try {
                for ((conflict, keep) in choices) {
                    if (keep) onResolveKeepMine(conflict) else onResolveKeepTheirs(conflict)
                }
                // the edit is unblocked now - push it right away instead of waiting for the
                // next auto-sync
                onResolutionFinished()
            } catch (e: Exception) {
                Log.w(
                    "TagConflictResolution",
                    "Resolving conflicts failed, unresolved ones stay pending", e
                )
            } finally {
                isApplying = false
            }
        }
    }

    val editType = group.first().editType
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = ::postpone,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = ::postpone,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("Cancel")
                }
                Text(
                    "Resolve Conflicts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = ::apply,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(editType.icon),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            // same category name the long-form quest title shows
                            // ("Sidewalks — Way #123"), so the user can relate this sheet back
                            // to the quest they answered; editType.title would be the question
                            // sentence ("Does this street have a sidewalk?") instead
                            Text(
                                (editType as? AddGenericLong)?.item?.elementType
                                    ?: stringResource(editType.title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            elementLabel?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        "This element was changed by someone else while you were answering, so " +
                        "your answer has not been submitted yet. Choose which value to keep for " +
                        "each question below - everything is submitted together once you confirm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            group.forEach { conflict ->
                Spacer(Modifier.height(20.dp))

                // the long-form question this tag came from reads much better than the raw OSM
                // key; the key is still shown underneath so the value can be traced in OSM data
                val question = questionTitleFor(conflict)
                Text(
                    question ?: conflict.tagKey.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                if (question != null) {
                    Text(
                        conflict.tagKey.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))

                val mine = keepMine[conflict.id] ?: true
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConflictChoiceRow(
                        text = "Your answer: ${conflict.mineValue ?: "(removed)"}",
                        selected = mine,
                        onClick = { keepMine[conflict.id] = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    ConflictChoiceRow(
                        text = "Existing value: ${conflict.theirsValueAtDetection ?: "(removed)"}",
                        selected = !mine,
                        onClick = { keepMine[conflict.id] = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The long-form question text the conflicting tag was answered through, if this conflict came
 *  from a long-form quest ([AddGenericLong] carries its workspace-defined question catalog) */
private fun questionTitleFor(conflict: PendingTagConflict): String? =
    (conflict.editType as? AddGenericLong)?.item?.quests
        ?.firstOrNull { it?.questTag == conflict.tagKey }
        ?.questTitle
