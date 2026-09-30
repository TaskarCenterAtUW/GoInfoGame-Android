package de.westnordost.streetcomplete.testutils

import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Captures a screenshot of the current screen into `/sdcard/test-screenshots/`, named after the
 * given test name.
 *
 * Call this from a test's `@After`, before any `ActivityScenario`/activity teardown runs (a
 * plain JUnit `@Rule` can't do this: `TestWatcher.succeeded`/`failed` only fire after the whole
 * `@Before`+`@Test`+`@After` chain has already completed, so a rule-based screenshot would show
 * a blank/finished activity instead of the screen the test actually left behind).
 *
 * Deliberately shells out to `screencap` (like the whole-run final screenshot android-test.yml
 * already takes) rather than writing a Bitmap into the app's own external-files dir: AGP
 * uninstalls both the app and the test APK once `connectedDebugAndroidTest` finishes, which wipes
 * that app-scoped directory (`/sdcard/Android/data/<pkg>/files/...`) along with it - confirmed by
 * running this locally against a real emulator, where the app-scoped file never survived to the
 * `adb pull` step. `/sdcard/test-screenshots/` isn't tied to the app's package, so it survives.
 *
 * Pulled off the device into `test-artifacts/` by `android-test.yml`, alongside the whole-run
 * video that workflow already captures.
 */
object UiTestScreenshot {
    private const val DEVICE_DIR = "/sdcard/test-screenshots"

    fun capture(testName: String) {
        val safeName = testName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        try {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.executeShellCommand("mkdir -p $DEVICE_DIR").drain()
            automation.executeShellCommand("screencap -p $DEVICE_DIR/$safeName.png").drain()
        } catch (e: Exception) {
            Log.w("UiTestScreenshot", "Failed to capture screenshot for $testName", e)
        }
    }

    // executeShellCommand runs the command asynchronously - its output pipe has to be read (or
    // at least closed after the process exits) for the command to be guaranteed complete/flushed
    private fun ParcelFileDescriptor.drain() {
        ParcelFileDescriptor.AutoCloseInputStream(this).use { it.readBytes() }
    }
}
