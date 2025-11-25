package de.westnordost.streetcomplete.screens.main.accessibility

import UndoChangesBottomSheetContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.westnordost.streetcomplete.data.edithistory.Edit
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.screens.main.edithistory.EditHistoryViewModel
import de.westnordost.streetcomplete.screens.main.edithistory.getTitle

// ---------- Models ----------

data class UndoQuestItem(
    val id: String,
    val timeLabel: String,   // "05:45 PM"
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
    val bgColor = Color(0xFFF7F7FB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
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
                        color = Color(0xFF272848)
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Header text
            Text(
                text = "Undo your recent changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF363A5E)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Select the quest based on date and time for preview & revert",
                fontSize = 14.sp,
                color = Color(0xFF7A7F98)
            )

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFFE2E1EC)
            )

            // List
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
                            onDismiss = { viewModel.select(null) },
                            viewModel,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                color = Color(0xFFE2E1EC)
            )

            // Bottom button
            OutlinedButton(
                onClick = onBackToPrevious,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.5.dp, Color(0xFF3C0E7A)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3C0E7A)
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

// ---------- Pieces ----------

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFB0B4C5)
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

    val label = item.edit?.getTitle(element?.tags) ?: "Unknown Quest"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFE3E4F0)),
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
                    color = Color(0xFF4E506B)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Type: $label",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF363A5E)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFB2B5C8)
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
                type = "Sidewalk",
                dateTime = "08 October 2025, 05:45 PM",
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

// ---------- Preview ----------

// @Composable
// @Preview(showBackground = true, showSystemUi = true)
// fun UndoEditsSectionPreview() {
//     val sampleSections = remember {
//         listOf(
//             UndoSection(
//                 dateLabel = "08 October 2025",
//                 items = listOf(
//                     UndoQuestItem("1", "05:45 PM", null, {}),
//                     UndoQuestItem("2", "05:45 PM", null, getElement),
//                     UndoQuestItem("3", "05:45 PM", null, getElement),
//                 )
//             ),
//             UndoSection(
//                 dateLabel = "07 October 2025",
//                 items = listOf(
//                     UndoQuestItem("4", "05:45 PM", null, getElement),
//                     UndoQuestItem("5", "05:45 PM", null, getElement),
//                     UndoQuestItem("6", "05:45 PM", null, getElement),
//                 )
//             )
//         )
//     }
//
//     MaterialTheme {
//         UndoEditsSection(sections = sampleSections)
//     }
// }

