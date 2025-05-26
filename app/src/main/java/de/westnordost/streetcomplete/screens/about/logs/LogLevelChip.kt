package de.westnordost.streetcomplete.screens.about.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.google.android.material.chip.Chip
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.logs.LogLevel
import de.westnordost.streetcomplete.ui.theme.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogLevelFilterChips(
    selectedLogLevels: Set<LogLevel>,
    onSelectedLogLevels: (Set<LogLevel>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.label_log_level),
            style = MaterialTheme.typography.labelMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (logLevel in LogLevel.entries) {
                LogLevelFilterChip(
                    logLevel = logLevel,
                    selected = logLevel in selectedLogLevels,
                    onClick = {
                        onSelectedLogLevels(
                            if (logLevel in selectedLogLevels) selectedLogLevels - logLevel
                            else selectedLogLevels + logLevel
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LogLevelFilterChip(
    logLevel: LogLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (selected) R.drawable.ic_check_circle_24dp else R.drawable.ic_circle_outline_24dp
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(logLevel.name)
        },
        leadingIcon = { Icon(painterResource(icon), null) },
    )
}

val LogLevel.color: Color @Composable get() = when (this) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.tertiary
    LogLevel.DEBUG -> MaterialTheme.colorScheme.tertiary
    LogLevel.INFO -> MaterialTheme.colorScheme.tertiary
    LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
    LogLevel.ERROR -> MaterialTheme.colorScheme.error
}

@PreviewLightDark
@Composable
private fun LogLevelFilterChipsPreview() {
    AppTheme { Surface {
        var logLevels by remember { mutableStateOf(LogLevel.entries.toSet()) }
        LogLevelFilterChips(
            selectedLogLevels = logLevels,
            onSelectedLogLevels = { logLevels = it }
        )
    } }
}
