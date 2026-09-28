package de.westnordost.streetcomplete.screens.main

import android.Manifest
import android.app.Activity
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.work.WorkManager
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.download.DownloadProgressSource
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsTable
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsController
import de.westnordost.streetcomplete.data.osm.edits.update_tags.PendingTagConflictsTable
import de.westnordost.streetcomplete.data.osm.edits.upload.changesets.OpenChangesetsTable
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.preferences.Autosync
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.quest.VisibleQuestsSource
import de.westnordost.streetcomplete.data.upload.UploadProgressSource
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.quests.sidewalk_long_form.fieldOf
import de.westnordost.streetcomplete.quests.sidewalk_long_form.scrollIntoView
import de.westnordost.streetcomplete.quests.sidewalk_long_form.tileOf
import de.westnordost.streetcomplete.screens.main.map.MainMapFragment
import de.westnordost.streetcomplete.screens.workspaces.FakeWorkspaceRepository
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.testutils.MockGps
import de.westnordost.streetcomplete.testutils.MockOsmServer
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockNode
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockWay
import de.westnordost.streetcomplete.testutils.UiTestScreenshot
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * A long-form answer given on the map is uploaded - changeset created, change uploaded, changeset
 * closed - and what happens when someone else edited the same sidewalk meanwhile, in each of the
 * workspace's conflict modes (`overrideConflicts`). See [MapSyncTestBase] for the setup.
 */
@RunWith(AndroidJUnit4::class)
class LongFormMapSyncFlowTest : MapSyncTestBase() {

    //region tests

    @Test
    fun answeringOnTheMap_uploadsInItsOwnChangeset_whichIsClosedAfterwards() {
        openWorkspaceAndSidewalkQuest()

        answerConcreteAndWidth60()

        val upload = awaitUploads(1).single()
        val changeset = MockOsmServer.snapshot().changesets.single()
        assertEquals(changeset.id, upload.changesetId)
        val way = upload.modifiedWays.single()
        assertEquals(SIDEWALK.id, way.id)
        assertEquals(1, way.version)
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)

        assertEquals("survey", changeset.tags["source"])
        assertTrue(changeset.tags.containsKey("comment"))
        assertTrue(changeset.tags.containsKey("created_by"))
        awaitChangesetsClosed()
        assertNoUnexpectedRequests()
    }

    // someone else edited a tag the user didn't answer: not a conflict in any mode
    @Test
    fun concurrentEditOfAnotherTag_isMerged_withoutAsking() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("lit" to "yes"))

        answerConcreteAndWidth60()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(2, way.version)
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60", "lit" to "yes"), way.tags)
        assertEquals(1, MockOsmServer.snapshot().rejectedUploads.size)
        assertNoConflictDialog()
        awaitChangesetsClosed()
        assertNoUnexpectedRequests()
    }

    //region RESOLVE mode (overrideConflicts false or missing): the user decides per tag

    @Test
    fun resolveMode_conflict_asksTheUser_andNothingIsUploadedUntilThen() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))

        answerConcreteAndWidth60()

        awaitConflictDialog()
        composeTestRule.onNode(hasText(SURFACE_Q)).assertExists()
        composeTestRule.onNode(hasText("Your answer: concrete")).assertExists()
        composeTestRule.onNode(hasText("Existing value: gravel")).assertExists()
        // the stale upload was rejected, and the edit is held - nothing of it is uploaded
        assertEquals(1, MockOsmServer.snapshot().rejectedUploads.size)
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        awaitChangesetsClosed()
    }

    @Test
    fun resolveMode_keepMine_uploadsTheAnswerOnTopOfTheOtherEdit() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()

        awaitConflictDialog()
        // "your answer" is preselected
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(2, way.version)
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
        awaitChangesetsClosed()
        assertNoUnexpectedRequests()
    }

    @Test
    fun resolveMode_keepExisting_uploadsTheRestOfTheAnswer() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()

        awaitConflictDialog()
        composeTestRule.onNode(hasText("Existing value: gravel")).performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(SIDEWALK.tags + mapOf("surface" to "gravel", "width" to "60"), way.tags)
        awaitChangesetsClosed()
    }

    @Test
    fun resolveMode_cancel_keepsTheEditHeld() {
        overrideConflicts = "false"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))
        answerConcreteAndWidth60()

        awaitConflictDialog()
        composeTestRule.onNodeWithText("Cancel").performClick()
        waitUntil("dialog closed") {
            composeTestRule.onAllNodes(hasText(CONFLICT_DIALOG_TITLE)).fetchSemanticsNodes().isEmpty()
        }

        SystemClock.sleep(3_000) // a (wrong) upload would have happened by now
        assertEquals(emptyList<MockOsmServer.Upload>(), MockOsmServer.snapshot().uploads)
        assertEquals(1, koin.get<PendingTagConflictsController>().getCount())
    }

    //endregion

    //region OVERRIDE mode (overrideConflicts true): the app's answer wins, no dialog

    @Test
    fun overrideMode_conflict_uploadsTheAnswer_withoutAsking() {
        overrideConflicts = "true"
        openWorkspaceAndSidewalkQuest()
        MockOsmServer.editWayConcurrently(SIDEWALK.id, mapOf("surface" to "gravel"))

        answerConcreteAndWidth60()

        val way = awaitUploads(1).single().modifiedWays.single()
        assertEquals(2, way.version)
        assertEquals(SIDEWALK.tags + mapOf("surface" to "concrete", "width" to "60"), way.tags)
        assertNoConflictDialog()
        assertEquals(0, koin.get<PendingTagConflictsController>().getCount())
        awaitChangesetsClosed()
        assertNoUnexpectedRequests()
    }

    //endregion

    //endregion
}
