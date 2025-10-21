package de.westnordost.streetcomplete.screens.main.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.ic_email_24
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Notification shown usually shown on the top end corner of e.g. a button */
@Composable
fun NotificationBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalTextStyle provides MaterialTheme.typography.labelSmall,
    ) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                .padding(vertical = 2.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Preview
@Composable
private fun PreviewMapButtonWithNotification() {
    Box {
        MapButton(onClick = {}) {
            Icon(painterResource(Res.drawable.ic_email_24), null)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            NotificationBox { Text(text = "999") }
        }
    }
}
