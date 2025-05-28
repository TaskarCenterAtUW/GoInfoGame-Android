package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R

@Composable
fun MapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable() (BoxScope.() -> Unit)
) {
    Surface(
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(Modifier.padding(16.dp), content = content)
    }
}

@Preview
@Composable
private fun PreviewMapButton() {
    MapButton(onClick = {}) {
        Icon(painterResource(R.drawable.ic_location_24dp), null)
    }
}
