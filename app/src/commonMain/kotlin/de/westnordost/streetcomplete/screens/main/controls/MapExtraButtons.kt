package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.action_imagery_list
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DownloadMapDataButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = 8.dp
    ) {
        Image(
            imageVector = Icons.Default.Download,
            contentDescription = "Fetch the latest quests for this area",
            modifier = Modifier
                .size(32.dp),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        )
    }
}

@Composable
fun ImageryListButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = 8.dp
    ) {
        Image(
            imageVector = Icons.Default.Layers,
            contentDescription = stringResource(Res.string.action_imagery_list),
            modifier = Modifier
                .size(32.dp),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        )
    }
}

@Preview
@Composable
private fun PreviewDownloadMapDataButton() {
    DownloadMapDataButton(
        onClick = {}
    )
}
