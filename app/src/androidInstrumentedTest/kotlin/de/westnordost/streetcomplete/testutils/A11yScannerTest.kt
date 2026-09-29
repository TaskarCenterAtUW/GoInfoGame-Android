package de.westnordost.streetcomplete.testutils

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.westnordost.streetcomplete.R
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves [A11yScanner] actually finds problems before its report is trusted: a screen with
 * known problems planted in plain Views and in Compose - an unlabeled image button, low-contrast
 * text, a tiny tap target - must each be reported.
 */
@RunWith(AndroidJUnit4::class)
class A11yScannerTest {

    @Test
    fun reportsKnownProblems_inViewsAndCompose() {
        val scenario = launchFragmentInContainer<PlantedProblemsFragment>(themeResId = R.style.AppTheme)
        try {
            val findings = A11yScanner.scan("Scanner self-test (planted problems)")
            val summary = findings.joinToString("\n") { "${it["check"]} ${it["class"]} '${it["text"]}' '${it["contentDescription"]}'" }

            fun has(check: String, where: (JSONObject) -> Boolean) =
                assertTrue("expected $check, got:\n$summary", findings.any { it["check"] == check && where(it) })

            // Views
            has("SpeakableTextPresentCheck") { it.getString("class").endsWith("ImageButton") }
            has("TextContrastCheck") { it["text"] == VIEW_FAINT_TEXT }
            has("TouchTargetSizeCheck") { it["contentDescription"] == VIEW_TINY }
            // Compose
            has("TextContrastCheck") { it["text"] == COMPOSE_FAINT_TEXT }
            has("TouchTargetSizeCheck") { it["contentDescription"] == COMPOSE_TINY }
            assertTrue(
                "expected the unlabeled clickable Compose image too, got:\n$summary",
                findings.count { it["check"] == "SpeakableTextPresentCheck" } >= 2
            )
        } finally {
            scenario.close()
        }
    }

    class PlantedProblemsFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            val context = requireContext()
            fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                addView(ImageButton(context).apply { setImageResource(android.R.drawable.ic_menu_camera) }, dp(56), dp(56))
                addView(TextView(context).apply {
                    text = VIEW_FAINT_TEXT
                    textSize = 16f
                    setTextColor(Color.rgb(0xDD, 0xDD, 0xDD))
                })
                addView(View(context).apply {
                    contentDescription = VIEW_TINY
                    setBackgroundColor(Color.BLACK)
                    setOnClickListener { }
                }, dp(16), dp(16))
                addView(ComposeView(context).apply {
                    setContent {
                        Column(Modifier.background(androidx.compose.ui.graphics.Color.White)) {
                            Image(
                                painterResource(android.R.drawable.ic_menu_camera),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clickable { }
                            )
                            Text(COMPOSE_FAINT_TEXT, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color(0xFFDDDDDD))
                            Box(
                                Modifier.size(16.dp).background(androidx.compose.ui.graphics.Color.Black)
                                    .clickable { }.semantics { contentDescription = COMPOSE_TINY }
                            )
                        }
                    }
                })
            }
        }
    }

    private companion object {
        const val VIEW_FAINT_TEXT = "Faint view text"
        const val VIEW_TINY = "tiny view target"
        const val COMPOSE_FAINT_TEXT = "Faint compose text"
        const val COMPOSE_TINY = "tiny compose target"
    }
}
