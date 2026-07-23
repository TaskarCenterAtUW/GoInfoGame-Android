package de.westnordost.streetcomplete.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular avatar showing a person's initials (e.g. "Rajesh Kumar" -> "RK"), falling back to a
 * generic person icon when [name] has no usable words (null/blank).
 */
@Composable
fun UserInitialsAvatar(
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val initials = remember(name) { initialsOf(name) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape)
    ) {
        if (initials != null) {
            Text(
                text = initials,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                // proportional to size rather than a fixed style, so this stays legible whether
                // it's a small toolbar avatar or the large one on the profile screen
                fontSize = (size.value * 0.4f).sp,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

private fun initialsOf(name: String?): String? {
    val words = name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
    if (words.isNullOrEmpty()) return null
    val first = words.first().first().uppercaseChar()
    val last = if (words.size > 1) words.last().first().uppercaseChar() else null
    return if (last != null) "$first$last" else first.toString()
}
