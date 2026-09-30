package de.westnordost.streetcomplete.testutils

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records the text of every toast shown while it is open, so tests can assert on them.
 *
 * Espresso can't see toasts: since Android 11, text toasts are drawn by the system in its own
 * window, outside the app's view hierarchy. But showing one still sends an accessibility
 * notification event carrying its text, which the instrumentation's UiAutomation receives no
 * matter which window drew it.
 *
 * Create it before the action that should show the toast, and [close] it afterwards (only one
 * accessibility listener can be set on the UiAutomation at a time).
 */
class ToastWatcher : AutoCloseable {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val shown = CopyOnWriteArrayList<String>()

    init {
        automation.setOnAccessibilityEventListener { event ->
            if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                event.className?.toString() == Toast::class.java.name
            ) {
                // Android 12+ adds the app's name as a second text part
                shown.add(event.text.joinToString(" "))
            }
        }
    }

    /** Toast texts seen so far, oldest first. */
    val texts: List<String> get() = shown.toList()

    /** Waits until a toast containing [text] has been shown, failing after [timeoutMillis]. */
    fun awaitToast(text: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (shown.any { it.contains(text) }) return
            Thread.sleep(50)
        }
        fail("expected a toast containing \"$text\", toasts shown: $shown")
    }

    override fun close() {
        automation.setOnAccessibilityEventListener(null)
    }
}
