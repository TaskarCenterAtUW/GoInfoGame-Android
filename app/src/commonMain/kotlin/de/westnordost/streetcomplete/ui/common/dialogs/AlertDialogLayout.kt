package de.westnordost.streetcomplete.ui.common.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The layout used in alert dialogs. Used for mimicking the appearance of alert dialogs when not
 *  using AlertDialog */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertDialogLayout(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    buttons: (@Composable FlowRowScope.() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Column(Modifier.padding(top = 24.dp)) {
            if (title != null) {
                val currentColor = LocalContentColor.current
                CompositionLocalProvider(
                    LocalContentColor provides currentColor.copy(alpha = 1f),
                    LocalTextStyle provides MaterialTheme.typography.titleMedium
                ) {
                    Column(Modifier.padding(start = 24.dp, bottom = 16.dp, end = 24.dp)) {
                        title()
                    }
                }
            }
            if (content != null) {
                val currentColor = LocalContentColor.current
                CompositionLocalProvider(
                    LocalContentColor provides currentColor.copy(alpha = 0.7f),
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    content()
                }
            }
            if (buttons != null) {
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) { buttons() }
            }
        }
    }
}
