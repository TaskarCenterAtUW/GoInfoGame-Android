package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val displayCountry = remember {
        viewModel.currentCountry?.let { getCountryName(it) } ?: "Atlantis"
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight =  LocalWindowInfo.current.containerSize.height.dp * 0.85f
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(end = 40.dp)) {
                    Text(
                        text = "Choose which features to survey",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Show all hidden elements on the map by individual item or type",
                        style = MaterialTheme.typography.bodyMedium,
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
            )
        }
    }
}

private fun getCountryName(countryCode: String): String =
    // we don't use the language, but we need it for correct construction of the languageTag
    Locale("en-$countryCode").displayRegion ?: countryCode
