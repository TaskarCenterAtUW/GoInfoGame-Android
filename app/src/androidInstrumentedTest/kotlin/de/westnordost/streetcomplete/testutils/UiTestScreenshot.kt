package de.westnordost.streetcomplete.testutils

import android.graphics.Bitmap
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Captures a screenshot of the current screen into the target app's external-files dir, named
 * after the given test name.
 *
 * Call this from a test's `@After`, before any `ActivityScenario`/activity teardown runs (a
 * plain JUnit `@Rule` can't do this: `TestWatcher.succeeded`/`failed` only fire after the whole
 * `@Before`+`@Test`+`@After` chain has already completed, so a rule-based screenshot would show
 * a blank/finished activity instead of the screen the test actually left behind).
 *
 * Pulled off the device into `test-artifacts/` by `android-test.yml`, alongside the whole-run
 * video that workflow already captures.
 */
object UiTestScreenshot {
    fun capture(testName: String) {
        try {
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
            val dir = File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                "screenshots"
            ).apply { mkdirs() }
            FileOutputStream(File(dir, "$testName.png")).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.w("UiTestScreenshot", "Failed to capture screenshot for $testName", e)
        }
    }
}
