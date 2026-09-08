package de.westnordost.streetcomplete.util.locale

import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalDateTimeFormatterTest {
    @Test fun format() {
        val german = Locale("de")
        val dateTime = LocalDateTime(LocalDate(1985, 11, 8), LocalTime(18, 30, 24))

        assertEquals(
            "08.11.85, 18:30",
            LocalDateTimeFormatter(german, dateStyle = DateFormatStyle.Short).format(dateTime)
        )
        assertEquals(
            "08.11.1985, 18:30:24",
            LocalDateTimeFormatter(german, dateStyle = DateFormatStyle.Medium).format(dateTime)
        )
        assertEquals(
            "8. November 1985, 18:30:24 MEZ",
            LocalDateTimeFormatter(german, timeZone = TimeZone.of("CET"), dateStyle = DateFormatStyle.Long).format(dateTime)
        )
        // the exact CET display name (e.g. "Mitteleuropäische Zeit" vs "...Normalzeit") comes from
        // the JDK's own CLDR locale data and differs between JDK versions (confirmed: JDK 21
        // produces "Zeit", JDK 24 produces "Normalzeit") - assert the part this formatter is
        // actually responsible for exactly, and only check the platform-supplied timezone name
        // loosely
        val full = LocalDateTimeFormatter(german, timeZone = TimeZone.of("CET"), dateStyle = DateFormatStyle.Full).format(dateTime)
        assertTrue(full.startsWith("Freitag, 8. November 1985, 18:30:24 Mitteleuropäische"))
    }
}
