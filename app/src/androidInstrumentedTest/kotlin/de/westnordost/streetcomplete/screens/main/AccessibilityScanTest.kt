package de.westnordost.streetcomplete.screens.main

import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.upload.UploadProgressSource
import de.westnordost.streetcomplete.quests.sidewalk_long_form.inRowOf
import de.westnordost.streetcomplete.quests.sidewalk_long_form.scrollIntoView
import de.westnordost.streetcomplete.screens.user.UserActivity
import de.westnordost.streetcomplete.screens.workspaces.FakeWorkspaceRepository
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.testutils.A11yScanner
import de.westnordost.streetcomplete.testutils.MockKartaView
import de.westnordost.streetcomplete.testutils.MockOsmServer
import de.westnordost.streetcomplete.testutils.MockOsmServer.Failure
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Report-only accessibility scans (WCAG 2.1 AA) of every screen and state users can reach - see
 * [A11yScanner]. These tests only drive the app to each screen and record what the scanner finds;
 * they don't fail on findings. The report is built with tools/a11y_report.py.
 *
 * Not covered on purpose (not visible to users currently): move node, split way, search features,
 * and Settings (its only entry, MainMenuDialog, isn't reachable - the toolbar's menu button opens the
 * quest filters).
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityScanTest : MapSyncTestBase() {

    //region login and workspaces

    @Test
    fun login() {
        logOut()
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText("Email")
        scan("Login")

        fakeRepository.login = { flow { throw Exception("Invalid username or password.") } }
        composeTestRule.onNodeWithText("Email").performTextInput("someone@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("wrong")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("Login").performClick()
        waitForText("Invalid username or password.")
        scan("Login (wrong credentials)")
    }

    @Test
    fun sessionExpired() {
        logOut()
        scenario = ActivityScenario.launch(
            Intent(InstrumentationRegistry.getInstrumentation().targetContext, WorkSpaceActivity::class.java)
                .putExtra(WorkSpaceActivity.SHOW_LOGGED_OUT_ALERT, true)
        )
        waitForText("Session Expired")
        scan("Session expired alert")
    }

    @Test
    fun workspaceList() {
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        scan("Workspace list")

        composeTestRule.onNodeWithContentDescription("Search workspaces").performClick()
        SystemClock.sleep(500)
        scan("Workspace list (search open)")
    }

    @Test
    fun workspaceListEmpty() {
        fakeRepository.workspaces = { flowOf(emptyList()) }
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitUntil("list loaded") { isShown("Navigate to profile screen") || isShownDesc("Navigate to profile screen") }
        SystemClock.sleep(3_000)
        scan("Workspace list (no workspaces nearby)")
    }

    @Test
    fun workspaceListError() {
        fakeRepository.workspaces = { flow { throw Exception("The server is temporarily unavailable. Please try again later.") } }
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText("temporarily unavailable")
        scan("Workspace list (could not load)")
    }

    @Test
    fun workspaceFailsToOpen() {
        fakeRepository.workspaceDetails = { flow { throw Exception("Failed. Workspace not found with ID : 1") } }
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).performClick()
        waitForText("Workspace not found")
        scan("Workspace list (workspace could not be opened)")
    }

    @Test
    fun profile() {
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithContentDescription("Navigate to profile screen").performClick()
        awaitActivity(UserActivity::class.java)
        SystemClock.sleep(1_500)
        scan("Profile")
    }

    //endregion

    //region map screen

    @Test
    fun mapScreen() {
        val activity = openWorkspace()
        awaitDownload()
        zoomTo(activity, awaitSidewalkQuest().position)
        scan("Map screen")
    }

    @Test
    fun questFilters() {
        openWorkspace()
        awaitDownload()
        onView(withId(R.id.mainMenuButton)).perform(click())
        waitForText("Manage Quests")
        scan("Quest filters (Manage Quests)")
    }

    @Test
    fun switchWorkspaceDialog() {
        openWorkspace()
        awaitDownload()
        onView(withId(R.id.workspace_container)).perform(click())
        SystemClock.sleep(800)
        scan("Switch workspace dialog")
    }

    @Test
    fun mapLongPress_createNoteAndFeature() {
        val activity = openWorkspace()
        awaitDownload()
        zoomTo(activity, awaitSidewalkQuest().position)
        longPressEmptyMap(activity)
        scan("Map long-press menu")

        onView(withText("Create new note")).perform(click())
        SystemClock.sleep(1_500)
        scan("Create note")
        Espresso.pressBack()
        SystemClock.sleep(800)

        longPressEmptyMap(activity)
        onView(withText("Add new feature")).perform(click())
        SystemClock.sleep(1_500)
        scan("Add new feature")
    }

    @Test
    fun mapControls() {
        val activity = openWorkspace()
        awaitDownload()
        zoomTo(activity, awaitSidewalkQuest().position)
        composeTestRule.onNodeWithContentDescription("Map Attribution").performClick()
        SystemClock.sleep(800)
        scan("Map attribution")

        composeTestRule.onNodeWithContentDescription("Fetch the latest quests for this area").performClick()
        SystemClock.sleep(800)
        scan("Map (after tapping download)")
    }

    @Test
    fun undoOnMap() {
        openWorkspaceAndSidewalkQuest()
        answerConcreteAndWidth60()
        awaitUploads(1)
        awaitSyncIdle() // the button is disabled while uploading/downloading
        composeTestRule.onNodeWithContentDescription("Undo edits").performClick()
        // the sidebar lists edits as date/time + icon only (no text label)
        val timeLabel = hasText(" PM", substring = true) or hasText(" AM", substring = true)
        waitUntil("edit history sidebar") { composeTestRule.onAllNodes(timeLabel).fetchSemanticsNodes().isNotEmpty() }
        SystemClock.sleep(500)
        scan("Edit history (from map)")

        // what TalkBack's actions menu does on the (selected) edit item: its "Undo this edit" action
        val item = composeTestRule.onNode(hasContentDescription(" edit", substring = true) and isSelected())
            .fetchSemanticsNode()
        val undo = item.config[SemanticsActions.CustomActions].single { it.label == "Undo this edit" }
        composeTestRule.runOnUiThread { undo.action() }
        waitUntil("undo dialog") { isShownDesc("Edit details") }
        scan("Undo dialog (from map)")
    }

    //endregion

    //region long form

    @Test
    fun longForm() {
        openWorkspaceAndSidewalkQuest()
        scan("Long form (half open)")
        expandSheet()
        scan("Long form (expanded)")

        typeInto(WIDTH_Q, "5")
        scan("Long form (number out of range)")
    }

    @Test
    fun longFormPhotoStates() {
        Intents.init()
        try {
            openWorkspaceAndSidewalkQuest()
            tapTile(OBSTRUCTION_Q, "Other obstruction")
            scan("Long form (photo prompt)")
            capturePhoto()
            scan("Long form (photo attached)")
        } finally {
            Intents.release()
        }
    }

    @Test
    fun longFormPhotoFromLastVisit() {
        MockOsmServer.put(SIDEWALK.copy(tags = SIDEWALK.tags + mapOf("obstacle" to "other", "ext:kartaview_url" to "https://storage.kartaview.org/files/photo/lth/x.jpg")))
        openWorkspaceAndSidewalkQuest()
        expandSheet()
        onView(inRowOf(OBSTRUCTION_Q, R.id.photo_title)).perform(scrollIntoView())
        scan("Long form (photo from last visit)")
        onView(inRowOf(OBSTRUCTION_Q, R.id.photo_delete)).perform(scrollIntoView(), click())
        scan("Long form (photo marked for removal)")
    }

    @Test
    fun multiSelectBar() {
        val activity = openWorkspace()
        awaitDownload()
        val quest = awaitSidewalkQuest()
        zoomTo(activity, quest.position)
        val (x, y) = pinOnScreen(activity, quest)
        press(x, y, long = true)
        SystemClock.sleep(1_000)
        scan("Multi-select bar")
    }

    //endregion

    //region dialogs and notices

    @Test
    fun conflictDialog() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel", "width" to "55"))
        answerConcreteAndWidth60()
        awaitConflictDialog()
        scan("Conflict dialog")
    }

    @Test
    fun discardedAnswerNotice() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.deleteWay(SIDEWALK.id)
        answerConcreteAndWidth60()
        waitForText("Answer could not be saved")
        scan("Answer could not be saved notice")
    }

    @Test
    fun uploadErrorDialog() {
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.failure = { if (it.endsWith("/upload")) Failure.Status(HttpStatusCode.BadRequest) else null }
        answerConcreteAndWidth60()
        waitForText("Upload error")
        scan("Upload error dialog")
    }

    @Test
    fun stuckPhotoNotice() {
        Intents.init()
        try {
            MockKartaView.failure = MockKartaView.Failure.Status(HttpStatusCode.ServiceUnavailable)
            openWorkspaceAndSidewalkQuest()
            tapTile(OBSTRUCTION_Q, "Other obstruction")
            capturePhoto()
            submit()
            val uploads: UploadProgressSource = koin.get()
            for (run in 1..3) {
                if (run > 1) tapUploadButton()
                waitUntil("photo upload attempt $run") { MockKartaView.requests.size >= run && !uploads.isUploadInProgress }
            }
            waitForText("Photo upload failed")
            scan("Photo upload failed notice")
        } finally {
            Intents.release()
        }
    }

    //endregion

    //region Follow mode ("Screen Reader Mode")

    @Test
    fun followMode() {
        openWorkspace()
        awaitDownload()
        awaitSidewalkQuest()
        onView(withId(R.id.followModeButton)).perform(click())
        waitForText("Screen Reader Mode")
        waitForText("Nearest Quests")
        SystemClock.sleep(1_000)
        scan("Follow mode (nearest quests)")

        composeTestRule.onAllNodes(hasText("Sidewalks", substring = true)).onFirst().performClick()
        waitForText("Start answering the questions")
        scan("Follow mode (quest selected)")

        if (isShown("Start answering the questions")) {
            composeTestRule.onNodeWithText("Start answering the questions").performClick()
            waitUntil("long form opened") { isDisplayed(SURFACE_Q) }
            scan("Follow mode (answering)")
        }
    }

    @Test
    fun followModeNoQuests() {
        MockOsmServer.deleteWay(SIDEWALK.id)
        openWorkspace()
        awaitDownload()
        onView(withId(R.id.followModeButton)).perform(click())
        waitForText("No Quests Found")
        scan("Follow mode (no quests)")
    }

    @Test
    fun followModeUndo() {
        openWorkspaceAndSidewalkQuest()
        answerConcreteAndWidth60()
        awaitUploads(1)
        SystemClock.sleep(1_000)
        onView(withId(R.id.followModeButton)).perform(click())
        waitForText("Screen Reader Mode")
        waitForText("Undo Edits")
        composeTestRule.onAllNodesWithText("Undo Edits").onFirst().performClick()
        SystemClock.sleep(1_500)
        scan("Follow mode (undo edits)")

        composeTestRule.onAllNodes(hasText("Sidewalks", substring = true)).onFirst().performClick()
        waitForText("Undo the following changes?")
        scan("Follow mode (undo an edit)")
    }

    //endregion

    //region helpers

    private fun scan(screen: String) {
        SystemClock.sleep(300) // let the last frame settle
        A11yScanner.scan(screen)
    }

    private fun awaitSyncIdle() {
        val uploads: UploadProgressSource = koin.get()
        val downloads: de.westnordost.streetcomplete.data.download.DownloadProgressSource = koin.get()
        waitUntil("no upload/download running") { !uploads.isUploadInProgress && !downloads.isDownloadInProgress }
        SystemClock.sleep(500)
    }

    private fun awaitDownload() {
        waitUntil("map data downloaded") { MockOsmServer.snapshot().requests.any { it == "GET map" } }
    }

    private fun logOut() {
        preferences.workspaceLogin = false
        preferences.workspaceToken = null
        preferences.workspaceRefreshToken = null
        preferences.workspaceUserId = null
    }

    private fun isShownDesc(description: String): Boolean =
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    /** A long press on map away from the pins (lower left of the map). */
    private fun longPressEmptyMap(activity: MainActivity) {
        var x = 0f
        var y = 0f
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = activity.supportFragmentManager.findFragmentById(R.id.mapFragment)!!.requireView()
            val location = IntArray(2).also { view.getLocationOnScreen(it) }
            x = location[0] + view.width * 0.25f
            y = location[1] + view.height * 0.75f
        }
        press(x, y, long = true)
        SystemClock.sleep(1_000)
    }

    //endregion
}
