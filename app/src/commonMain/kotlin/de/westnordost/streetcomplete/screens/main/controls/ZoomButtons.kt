package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.ui.common.ZoomInIcon
import de.westnordost.streetcomplete.ui.common.ZoomOutIcon
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Combined control for zooming in and out */
@Composable
fun ZoomButtons(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
    ) {
        Column(Modifier.width(IntrinsicSize.Min)) {
            IconButton(onClick = onZoomIn, enabled = enabled) { ZoomInIcon() }
            Divider()
            IconButton(onClick = onZoomOut, enabled = enabled) { ZoomOutIcon() }
        }
    }
}

@Preview
@Composable
private fun PreviewZoomButtons() {
    ZoomButtons(onZoomIn = {}, onZoomOut = {})
}
