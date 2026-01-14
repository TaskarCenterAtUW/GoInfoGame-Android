package de.westnordost.streetcomplete.screens.main.accessibility

import UndoChangesBottomSheetContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistoryViewModel
import de.westnordost.streetcomplete.screens.main.edithistory.getName
import de.westnordost.streetcomplete.screens.user.DottedDivider

// ---------- Models ----------

data class UndoQuestItem(
    val id: String,
    val timeLabel: String,   // "05:45 PM"
    val dateLabel: String,
    val edit: Edit?,
    val viewModel: EditHistoryViewModel,    // "Sidewalk", "Curb"
)

data class UndoSection(
    val dateLabel: String,           // "08 October 2025"
    val items: List<UndoQuestItem>,
)

// ---------- Screen ----------
@Composable
fun UndoEditsScreen(
    modifier: Modifier = Modifier,
    editHistoryViewModel: EditHistoryViewModel,
    onClose: () -> Unit,
) {
    val editItems by editHistoryViewModel.editItems.collectAsState()
    val selectedEdit by editHistoryViewModel.selectedEdit.collectAsState()
    val sections = remember(editItems) {
        // Group by local date (assumes each edit item has `time: Long` epoch millis, `id` and `typeLabel`)
        editItems.groupBy { item ->
            java.time.Instant.ofEpochMilli(item.edit.createdTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.toSortedMap(compareByDescending { it }) // newest date first
            .map { (date, items) ->
                val dateLabel =
                    date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                val questItems = items.map { e ->
                    val timeLabel = java.time.Instant.ofEpochMilli(e.edit.createdTimestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                    UndoQuestItem(
                        id = e.edit.key.toString(),
                        timeLabel = timeLabel,
                        dateLabel = dateLabel,
                        e.edit,
                        editHistoryViewModel
                    )
                }
                UndoSection(dateLabel = dateLabel, items = questItems)
            }
    }

    UndoEditsSection(
        selectedEdit,
        sections = sections,
        onClose = onClose,
        onItemClick = {},
        onBackToPrevious = onClose,
        editHistoryViewModel
    )
}

@Composable
fun UndoEditsSection(
    selectedEdit: Edit?,
    sections: List<UndoSection>,
    onClose: () -> Unit = {},
    onItemClick: (UndoQuestItem) -> Unit = {},
    onBackToPrevious: () -> Unit = {},
    viewModel: EditHistoryViewModel,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Undo Edits",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close undo edits screen"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Header text
            Text(
                text = "Undo your recent changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Select the quest based on date and time for preview & revert",
                fontSize = 14.sp
            )

            DottedDivider(
                modifier = Modifier.padding(vertical = 16.dp),
            )

            if (sections.isEmpty()) {
                NoEditsUI()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    sections.forEach { section ->
                        item(key = "header_${section.dateLabel}") {
                            DateHeader(section.dateLabel)
                        }
                        items(section.items, key = { it.id }) { item ->
                            UndoItemCard(
                                item = item,
                                isSelected = selectedEdit?.key == item.edit?.key,
                                onClick = {
                                    viewModel.select(item.edit?.key)
                                    onItemClick(item)
                                },
                                onDismiss = { viewModel.hideSidebar() },
                                viewModel,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {

            DottedDivider(
                modifier = Modifier.padding(vertical = 16.dp),
            )

            // Bottom button
            OutlinedButton(
                onClick = onBackToPrevious,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(30.dp),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    width = 1.5.dp,
                    brush = SolidColor(MaterialTheme.colorScheme.primary)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Go back to previous screen",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun NoEditsUI(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Edits Found!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "New edits will appear here when a quest is answered",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UndoItemCard(
    item: UndoQuestItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: EditHistoryViewModel,
) {
    var element by remember { mutableStateOf<Element?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    // suspend call: OK in LaunchedEffect
    LaunchedEffect(item.edit) {
        if (item.edit == null) return@LaunchedEffect
        element = viewModel.getEditElement(item.edit)
    }

    val label = item.edit?.getName() ?: "Unknown"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White),
        shadowElevation = 1.dp,
        onClick = {
            onClick()
            showSheet = true
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.timeLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Type: $label",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            UndoChangesBottomSheetContent(
                item.edit,
                type = label,
                dateTime = "${item.dateLabel} ${item.timeLabel}",
                onRevertClick = {
                    if (item.edit != null) {
                        viewModel.undo(item.edit.key)
                    }
                    showSheet = false
                },
                onCancelClick = { showSheet = false },
                onCloseClick = { showSheet = false },
            )
        }
    } else {
        onDismiss()
    }
}
