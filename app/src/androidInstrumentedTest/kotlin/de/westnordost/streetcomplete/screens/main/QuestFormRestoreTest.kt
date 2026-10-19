package de.westnordost.streetcomplete.screens.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A quest form is open, the app goes to the background and Android kills the process. When the
 * user comes back, MainActivity is recreated from its saved state, restoring the form - which looks
 * up its quest type in the registry. This used to crash (NPE in AbstractQuestForm.onCreate): the
 * fragments were restored in super.onCreate, before the long form's quest types were registered.
 */
@RunWith(AndroidJUnit4::class)
class QuestFormRestoreTest : MapSyncTestBase() {

    @Test
    fun questFormIsRestoredWhenMainActivityIsRecreatedInAFreshProcess() {
        val activity = openWorkspace()
        openSidewalkQuest(activity)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            // the registry as it is in a fresh process: empty until MainActivity fills it
            koin.get<QuestTypeRegistry>().addItem(emptyList())
            activity.recreate()
        }

        var recreated: MainActivity? = null
        waitUntil("MainActivity recreated") {
            instrumentation.runOnMainSync {
                recreated = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .firstOrNull { it !== activity }
            }
            recreated != null
        }
        waitUntil("restored long form shown") { isDisplayed(SURFACE_Q) }
    }
}
