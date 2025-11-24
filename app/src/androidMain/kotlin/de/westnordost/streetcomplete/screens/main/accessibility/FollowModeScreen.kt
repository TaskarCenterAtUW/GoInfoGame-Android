package de.westnordost.streetcomplete.screens.main.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.screens.main.map.MainMapFragment
import de.westnordost.streetcomplete.util.ktx.toLatLon
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FollowModeScreen(
    mapFragment: MainMapFragment,
    onClose: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onUndoEdits: () -> Unit = {},
    onBackToMap: () -> Unit = {},
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE4FFCF),
            Color(0xFFF4FFF0)
        )
    )

    val questsState = remember { mutableStateListOf<QuestUiModel>() }
    val refreshTrigger = remember { mutableStateOf(0) }

    LaunchedEffect(mapFragment, refreshTrigger.value) {
        // Get current quests in view and store in a remembered state so it's accessible elsewhere
        val loaded = mapFragment.questPinsManager?.getQuestsInViewSnapshot()?.map { quest ->
            QuestUiModel(
                id = quest.key.toString(),
                distanceMeters = getDistanceBetweenPoints(
                    mapFragment.displayedLocation!!.toLatLon(), quest.position
                ),
                questName = quest.type.name,
                direction = getDirectionFromBearing(mapFragment.displayedLocation?.bearing),
                onClick = {
                    mapFragment.listener?.onClickedQuest(quest.key)
                }
            )
        } ?: emptyList()
        val nearest = loaded.sortedBy { model -> model.distanceMeters }.take(5)
        questsState.clear()
        questsState.addAll(nearest)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Status-bar inset
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            TopBar(onClose = onClose)

            Spacer(Modifier.height(12.dp))

            // Foreground white sheet
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    HeaderRow(
                        questCount = questsState.size,
                        onRefresh = { refreshTrigger.value++ }
                    )

                    Divider(
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp),
                        color = Color(0xFFE4E0F5)
                    )

                    QuestList(
                        quests = questsState,
                        modifier = Modifier.weight(1f, fill = true)
                    )

                    Spacer(Modifier.height(16.dp))

                    BottomButtons(
                        onUndoEdits = onUndoEdits,
                        onBackToMap = onBackToMap
                    )
                }
            }
        }
    }
}

// Get direction as text from bearing in degrees
fun getDirectionFromBearing(bearing: Float?): String {
    return when (bearing) {
        null -> "Unknown"
        in 22.5f..67.5f -> "North-East"
        in 67.5f..112.5f -> "East"
        in 112.5f..157.5f -> "South-East"
        in 157.5f..202.5f -> "South"
        in 202.5f..247.5f -> "South-West"
        in 247.5f..292.5f -> "West"
        in 292.5f..337.5f -> "North-West"
        else -> "North"
    }
}

fun getDistanceBetweenPoints(point1: LatLon, point2: LatLon): Int {
    val R = 6371000.0 // Radius of the Earth in meters
    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLonRad = Math.toRadians(point2.longitude - point1.longitude)

    val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
        cos(lat1Rad) * cos(lat2Rad) *
        sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (R * c).roundToInt() // Distance in meters
}

// ---------- Models ----------

data class QuestUiModel(
    val id: String,
    val distanceMeters: Int,
    val questName: String,
    val direction: String,
    val onClick: () -> Unit = { },
) {
    val description: String
        get() = "You are $distanceMeters meters from '$questName',\n" +
            "to the $direction"
}

// ---------- Internal UI pieces ----------

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Accessibility Mode",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
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
}

@Composable
private fun HeaderRow(
    questCount: Int,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Nearest Quests: %02d".format(questCount),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF27125F)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Select the quest and start answering.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A6A7A)
            )
        }

        Spacer(Modifier.width(12.dp))

        FilledTonalButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF4E2C9F),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Refresh list")
        }
    }
}

@Composable
private fun QuestList(
    quests: List<QuestUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        quests.forEachIndexed { index, quest ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            QuestCard(quest)
        }
    }
}

@Composable
private fun QuestCard(quest: QuestUiModel) {
    var showSheet by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF3C0E7A),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        onClick = {
            showSheet = true
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF56C271)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    lineHeight = 20.sp
                )
            )
        }
    }

    if (showSheet) {
        QuestBottomSheet(
            selectedType = quest.questName,
            onStartAnswering = {
                showSheet = false
                quest.onClick()
            },
            onHideQuest = { },
            onNotNow = { showSheet = false },
            onClose = { showSheet = false }
        )
    }
}

@Composable
private fun BottomButtons(
    onUndoEdits: () -> Unit,
    onBackToMap: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onUndoEdits,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50)
        ) {
            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Undo Edits",
                textAlign = TextAlign.Center
            )
        }

        OutlinedButton(
            onClick = onBackToMap,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Go back to map view",
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------
// Bottom sheet composable
// ---------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestBottomSheet(
    selectedType: String,
    onStartAnswering: () -> Unit,
    onHideQuest: () -> Unit,
    onNotNow: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Type:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF3C3973)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = selectedType,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF3C0E7A)
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE2E1EC)
            )

            // Primary button
            Button(
                onClick = {
                    onStartAnswering()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3C0E7A),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Start answering the questions",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Outlined button
            OutlinedButton(
                onClick = {
                    onHideQuest()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3C0E7A)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp,
                    brush = SolidColor(Color(0xFF3C0E7A))
                )
            ) {
                Text(
                    text = "Hide this quest",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Text button at bottom
            TextButton(
                onClick = {
                    onNotNow()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Not now",
                    fontSize = 15.sp,
                    color = Color(0xFF3C0E7A),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
