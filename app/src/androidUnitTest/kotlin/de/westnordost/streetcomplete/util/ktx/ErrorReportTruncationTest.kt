package de.westnordost.streetcomplete.util.ktx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// regression: "send crash report" crashed with TransactionTooLargeException (3.9MB report, mostly
// the attached log) - the report must be cut down to fit in an intent extra
class ErrorReportTruncationTest {

    @Test fun `short report is sent unchanged`() {
        val report = "Stack trace:\nNullPointerException\nLog:\nsomething"
        assertEquals(report, report.truncatedForIntent())
    }

    @Test fun `report of exactly the maximum length is sent unchanged`() {
        val report = "x".repeat(MAX_REPORT_HEAD_LENGTH + MAX_REPORT_TAIL_LENGTH)
        assertEquals(report, report.truncatedForIntent())
    }

    @Test fun `huge report keeps stack trace and newest log lines`() {
        val head = "Stack trace:\njava.lang.NullPointerException\n\tat AbstractQuestForm.onCreate\nLog:\n"
        val newestLog = "\nnewest log line"
        val report = head + "old log line\n".repeat(300_000) + newestLog // ~3.9M chars

        val truncated = report.truncatedForIntent()

        val omitted = report.length - MAX_REPORT_HEAD_LENGTH - MAX_REPORT_TAIL_LENGTH
        assertTrue(truncated.startsWith(head))
        assertTrue(truncated.endsWith(newestLog))
        assertTrue(truncated.contains("[... $omitted characters omitted ...]"))
        // well within the ~1MB binder buffer (strings are parcelled as UTF-16)
        assertTrue(truncated.length * 2 < 500_000)
    }
}
