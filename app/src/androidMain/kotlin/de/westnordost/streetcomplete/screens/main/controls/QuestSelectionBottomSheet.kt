package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.westnordost.streetcomplete.screens.settings.quest_selection.HiddenQuestsSection
import de.westnordost.streetcomplete.screens.settings.quest_selection.QuestSelectionList
import de.westnordost.streetcomplete.screens.settings.quest_selection.QuestSelectionViewModel
import de.westnordost.streetcomplete.util.ktx.displayRegion

/** Bottom sheet in which the user can choose which quest types (features) to survey, shown from
 *  the map's filter options button */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestSelectionBottomSheet(
    viewModel: QuestSelectionViewModel,
    onClose: () -> Unit,
) {
    val filteredQuests by viewModel.filteredQuests.collectAsStateWithLifecycle()
    val hiddenQuests by viewModel.hiddenQuests.collectAsStateWithLifecycle()
    val displayCountry = remember {
        viewModel.currentCountry?.let { getCountryName(it) } ?: "Atlantis"
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // containerSize is in pixels - .dp on a raw Int does NOT do a px-to-dp conversion (it just
    // wraps the number as-is), so this must go through the actual screen density or the resulting
    // "80%" is several times larger than the real screen height and never actually constrains
    // anything, letting the sheet expand to fill the whole screen regardless of the fraction used.
    val density = LocalDensity.current
    val maxSheetHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    } * 0.8f
    // Swallow any scroll/fling leftover from the lists below so it never bubbles up into the
    // sheet's own drag-to-dismiss handling - only the sheet's drag handle should move the sheet
    val blockSheetDragFromContent = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(blockSheetDragFromContent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(end = 40.dp)) {
                    Text(
                        text = "Manage Quests",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Show or hide elements on the map by individual item or type",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            QuestSelectionList(
                items = filteredQuests,
                displayCountry = displayCountry,
                onSelect = { questType, selected -> viewModel.select(questType, selected) },
                onReorder = { questType, toAfter -> viewModel.order(questType, toAfter) },
                modifier = Modifier.weight(1f),
            )

            HiddenQuestsSection(
                items = hiddenQuests,
                onUnhide = { key -> viewModel.unhideQuest(key) },
                onUnhideAll = { viewModel.unhideAllQuests() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun getCountryName(countryCode: String): String =
    // we don't use the language, but we need it for correct construction of the languageTag
    Locale("en-$countryCode").displayRegion ?: countryCode
