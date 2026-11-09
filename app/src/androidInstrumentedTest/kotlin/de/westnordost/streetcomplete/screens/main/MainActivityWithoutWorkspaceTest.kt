package de.westnordost.streetcomplete.screens.main

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * MainActivity started without a workspace's long form - by the sync notification after the
 * activity was closed, or by a geo:/streetcomplete: link - used to crash with an NPE in
 * doLongForm. It goes to the workspace list instead.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityWithoutWorkspaceTest {

    @Before fun setUp() {
        Intents.init()
        // only record it, don't actually open the workspace list
        intending(hasComponent(WorkSpaceActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After fun tearDown() {
        Intents.release()
    }

    @Test fun startedLikeTheSyncNotificationGoesToWorkspaceList() {
        assertGoesToWorkspaceList(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    @Test fun startedByGeoLinkGoesToWorkspaceList() {
        assertGoesToWorkspaceList(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:47.6,-122.3"), context, MainActivity::class.java)
        )
    }

    private fun assertGoesToWorkspaceList(intent: Intent) {
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
        intended(hasComponent(WorkSpaceActivity::class.java.name))
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
}
