package de.westnordost.streetcomplete.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import de.westnordost.streetcomplete.R

val ProximaNovaFontFamily = FontFamily(
    Font(R.font.proximanova_regular, FontWeight.Normal),
    Font(R.font.proximanova_bold, FontWeight.Bold),
    Font(R.font.proximanova_light, FontWeight.Light),
    Font(R.font.proximanova_semibold, FontWeight.SemiBold)
)


val proximaTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = ProximaNovaFontFamily,
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = ProximaNovaFontFamily,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = ProximaNovaFontFamily,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = ProximaNovaFontFamily,
        fontWeight = FontWeight.Bold
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = ProximaNovaFontFamily,
        fontWeight = FontWeight.Bold
    )
)
