package de.westnordost.streetcomplete.testutils

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.BitmapImage
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Accessibility scan of whatever is on screen right now, the way TalkBack sees it: the
 * accessibility tree of every window of the app (activity, dialogs, bottom sheets - Views and
 * Compose alike), checked with Google's Accessibility Test Framework (the checks behind
 * Accessibility Scanner), plus a screenshot for the contrast checks.
 *
 * REPORT-ONLY: never fails the calling test. Each finding is appended as one JSON line to
 * `findings.jsonl` (with a screenshot per scan), mirrored to [DEVICE_DIR] - the app's own files are
 * deleted when the test run uninstalls it. tools/a11y_report.py turns that into the report,
 * grouped by screen and WCAG 2.1 criterion.
 */
object A11yScanner {
    const val TAG = "A11yScanner"
    /** Survives the app being uninstalled after the run - pull it from here. */
    const val DEVICE_DIR = "/sdcard/test-a11y"

    private val checks by lazy {
        AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)
    }

    /** Where the findings are written first (the test process can't write to [DEVICE_DIR]). */
    val outputDir: File by lazy {
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "a11y").apply { mkdirs() }
    }

    /**
     * Scans the screen as it is now and records the findings under [screen] (e.g. "Login",
     * "Long form / photo attached"). Returns the findings; never throws.
     */
    fun scan(screen: String): List<JSONObject> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return try {
            instrumentation.waitForIdleSync()
            val automation = instrumentation.uiAutomation
            // needed to see all windows (dialogs, bottom sheets), not just the focused one
            automation.serviceInfo = automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            val packageName = instrumentation.targetContext.packageName
            val windows = automation.windows.filter { it.root?.packageName == packageName }
            if (windows.isEmpty()) {
                record(screen, error = "no window of the app on screen")
                return emptyList()
            }
            val hierarchy = AccessibilityHierarchyAndroid.newBuilder(windows, instrumentation.targetContext).build()

            val parameters = Parameters()
            val screenshot = automation.takeScreenshot()
            if (screenshot != null) {
                // the contrast checks read pixels, which a HARDWARE bitmap doesn't allow
                val readable = screenshot.copy(Bitmap.Config.ARGB_8888, false)
                parameters.putScreenCapture(BitmapImage(readable))
                saveScreenshot(screen, readable)
            }

            val findings = mutableListOf<JSONObject>()
            for (check in checks) {
                for (result in check.runCheckOnHierarchy(hierarchy, null, parameters)) {
                    val type = result.type
                    if (type != AccessibilityCheckResultType.ERROR &&
                        type != AccessibilityCheckResultType.WARNING &&
                        type != AccessibilityCheckResultType.INFO) continue
                    val element = result.element
                    val bounds = element?.boundsInScreen
                    val checkName = check.javaClass.simpleName
                    val finding = JSONObject().apply {
                        put("check", checkName)
                        put("severity", type.name)
                        put("wcag", Wcag.criterionOf(checkName))
                        put("message", result.getMessage(Locale.ENGLISH).toString())
                        put("class", element?.className?.toString() ?: "")
                        put("resourceId", element?.resourceName ?: "")
                        put("text", element?.text?.toString() ?: "")
                        put("contentDescription", element?.contentDescription?.toString() ?: "")
                        put("bounds", bounds?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "")
                    }
                    record(screen, finding)
                    findings.add(finding)
                }
            }
            Log.i(TAG, "$screen: ${findings.size} finding(s)")
            mirrorToDevice()
            findings
        } catch (e: Throwable) {
            Log.e(TAG, "Scan of \"$screen\" failed", e)
            record(screen, error = e.toString())
            emptyList()
        }
    }

    private fun record(screen: String, finding: JSONObject? = null, error: String? = null) {
        val line = (finding ?: JSONObject()).apply {
            put("screen", screen)
            if (error != null) put("scanError", error)
        }
        synchronized(this) {
            File(outputDir, "findings.jsonl").appendText(line.toString() + "\n")
        }
    }

    private fun saveScreenshot(screen: String, bitmap: Bitmap) {
        val name = screen.replace(Regex("[^A-Za-z0-9_.-]+"), "_") + ".png"
        File(outputDir, "screens").apply { mkdirs() }.resolve(name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Copies the results to [DEVICE_DIR], as the shell user (like UiTestScreenshot's screencap). */
    private fun mirrorToDevice() {
        shell("mkdir -p $DEVICE_DIR/screens")
        shell("cp ${File(outputDir, "findings.jsonl").path} $DEVICE_DIR/findings.jsonl")
        shell("cp -r ${File(outputDir, "screens").path}/. $DEVICE_DIR/screens/")
    }

    private fun shell(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
}

/** The WCAG 2.1 criterion (or guideline) each ATF check stands for. */
object Wcag {
    private val byCheck = mapOf(
        "SpeakableTextPresentCheck" to "1.1.1 Non-text Content / 4.1.2 Name, Role, Value (A)",
        "EditableContentDescCheck" to "3.3.2 Labels or Instructions / 4.1.2 Name, Role, Value (A)",
        "TextContrastCheck" to "1.4.3 Contrast (Minimum) (AA)",
        "ImageContrastCheck" to "1.4.11 Non-text Contrast (AA)",
        "DuplicateSpeakableTextCheck" to "2.4.6 Headings and Labels (AA)",
        "RedundantDescriptionCheck" to "1.1.1 Non-text Content (A) - best practice",
        "DuplicateClickableBoundsCheck" to "4.1.2 Name, Role, Value (A) - best practice",
        "ClassNameCheck" to "4.1.2 Name, Role, Value (A)",
        "ClickableSpanCheck" to "2.1.1 Keyboard / 4.1.2 Name, Role, Value (A)",
        "TraversalOrderCheck" to "1.3.2 Meaningful Sequence / 2.4.3 Focus Order (A)",
        "LinkPurposeUnclearCheck" to "2.4.4 Link Purpose (In Context) (A)",
        "TextSizeCheck" to "1.4.4 Resize Text (AA)",
        "UnexposedTextCheck" to "1.1.1 Non-text Content / 1.3.1 Info and Relationships (A)",
        // 2.5.5 Target Size is AAA in WCAG 2.1 - reported as Android's 48dp guideline, not an AA failure
        "TouchTargetSizeCheck" to "Guideline: Android 48dp touch target (WCAG 2.5.5 is AAA)",
    )

    fun criterionOf(checkName: String): String = byCheck[checkName] ?: "Unmapped ($checkName)"
}
