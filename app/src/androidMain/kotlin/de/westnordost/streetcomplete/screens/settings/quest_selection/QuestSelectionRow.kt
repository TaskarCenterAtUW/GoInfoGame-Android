package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.quests.sidewalk_long_form.AddGenericLong
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.questList_disabled_by_default
import de.westnordost.streetcomplete.resources.questList_disabled_in_country
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Single item in the quest selection list. Shows icon + title, whether it is enabled and whether
 *  it is disabled by default / disabled in the country one is in */
@Composable
fun QuestSelectionRow(
    item: QuestSelection,
    onToggleSelection: (isSelected: Boolean) -> Unit,
    displayCountry: String,
    modifier: Modifier = Modifier,
) {
    val alpha = if (!item.selected) 0.5f else 1.0f
    var title = ""
    title = if (item.questType is AddGenericLong) {
        item.questType.item.elementType!!
    } else {
        "Create Note"
    }
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // if (item.isInteractionEnabled) {
        //     Icon(painterResource(Res.drawable.ic_drag_vertical_24), "Reorder")
        // } else {
        //     Spacer(Modifier.size(24.dp))
        // }
        Spacer(Modifier.size(24.dp))
        Image(
            painter = painterResource(item.questType.icon),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .alpha(alpha)
                .semantics { hideFromAccessibility() },
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(0.1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = title,
                modifier = Modifier
                    .alpha(alpha)
                    .clearAndSetSemantics {
                        hideFromAccessibility()
                    },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!item.enabledInCurrentCountry) {
                DisabledHint(
                    stringResource(
                        Res.string.questList_disabled_in_country,
                        displayCountry
                    )
                )
            }
            if (item.questType.defaultDisabledMessage != null) {
                DisabledHint(stringResource(Res.string.questList_disabled_by_default))
            }
        }
        Box(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = onToggleSelection,
                enabled = item.isInteractionEnabled,
                modifier = Modifier.semantics {
                    contentDescription = "Quest type : $title"
                }
            )
        }
    }
}

@Composable
private fun DisabledHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = LocalContentColor.current.copy(alpha = 0.6f),
    )
}

@Preview
@Composable
private fun QuestSelectionRowPreview() {
    var selected by remember { mutableStateOf(true) }

    QuestSelectionRow(
        item = QuestSelection(
            AddGenericLong(Elements(), recencyPeriodInDays = 90),
            selected,
            false
        ),
        onToggleSelection = { selected = !selected },
        displayCountry = "Atlantis",
    )
}
