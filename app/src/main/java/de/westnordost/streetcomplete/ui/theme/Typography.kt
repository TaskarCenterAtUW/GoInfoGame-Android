package de.westnordost.streetcomplete.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val material2 = Typography()

val Typography = Typography(
    headlineLarge = material2.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineSmall = material2.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = material2.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold))
    ),
    titleMedium = material2.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold))
    ),
    titleSmall = material2.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold))
    )
)
