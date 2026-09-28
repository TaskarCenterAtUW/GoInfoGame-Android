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
 * The whole path of a long-form answer to OSM, on the real map screen: logged in, the workspace
 * list shows, the user taps the workspace, MainActivity opens with its long form, the (mock) GPS
 * puts the user next to a sidewalk, the map data is downloaded, the sidewalk's quest pin is
 * tapped on the map, the form is answered and submitted, and the edit is uploaded - changeset
 * created, change uploaded, changeset closed. Plus what happens when someone else edited the same
 * sidewalk meanwhile, in each of the workspace's conflict modes (`overrideConflicts`).
 *
 * Everything past the fake login is the app's own code; the network is [MockOsmServer] (OSM API,
 * hooked into the app's own "osmClient") and a mock workspace API behind the real
 * [WorkspaceApiService] / [WorkspaceRepositoryImpl] / [WorkspaceDao].
 */
@RunWith(AndroidJUnit4::class)
class LongFormMapSyncFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *(if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        })
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testName = TestName()

    private val koin = GlobalContext.get()
    private val preferences: Preferences = koin.get()
    private val workspaceDao: WorkspaceDao = koin.get()
    private val gps = MockGps(USER_POSITION.latitude, USER_POSITION.longitude)
    private val fakeRepository = FakeWorkspaceRepository()
    private lateinit var scenario: ActivityScenario<WorkSpaceActivity>

    @Volatile private var overrideConflicts: String = "null"
    private var previousAnimationScales: List<String> = emptyList()

    @Before
    fun setUp() {
        clearAppState()
        saveValidSession()

        MockOsmServer.reset()
        MockOsmServer.put(*SIDEWALK_NODES.toTypedArray(), SIDEWALK)

        val realRepository = WorkspaceRepositoryImpl(workspaceApiService(), workspaceDao)
        fakeRepository.workspaces = { realRepository.getWorkspaces(it) }
        fakeRepository.workspaceDetails = { realRepository.getWorkspaceDetails(it) }
        loadKoinModules(module {
            single<WorkspaceRepository> { fakeRepository }
        })
        MockOsmServer.attachTo(koin.get(named("osmClient")))
        MockOsmServer.isActive = true

        gps.start()
        // same as LongFormFormTest: RecyclerView item animations leave two views of the same
        // tile around for a moment, which Espresso doesn't wait for
        previousAnimationScales = ANIMATION_SCALES.map { shell("settings get global $it").trim() }
        ANIMATION_SCALES.forEach { shell("settings put global $it 0") }
    }

    @After
    fun tearDown() {
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        gps.stop()
        ANIMATION_SCALES.zip(previousAnimationScales).forEach { (key, value) ->
            shell("settings put global $key ${value.takeIf { it != "null" } ?: "1"}")
        }
        if (::scenario.isInitialized) scenario.close()
        finishAllActivities()
        clearAppState()
        MockOsmServer.isActive = false
    }

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

    //region flow helpers

    private fun openWorkspaceAndSidewalkQuest() {
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).performClick()

        val activity = awaitActivity(MainActivity::class.java)
        waitUntil("map data downloaded") { MockOsmServer.snapshot().requests.any { it == "GET map" } }
        val quest = awaitSidewalkQuest()
        tapQuestPin(activity, quest)
        waitUntil("long form opened") { isDisplayed(SURFACE_Q) }
    }

    private fun awaitSidewalkQuest(): Quest {
        val source: VisibleQuestsSource = koin.get()
        var quest: Quest? = null
        waitUntil("sidewalk quest on the map") {
            quest = source.getAll(AREA).firstOrNull {
                val key = it.key
                key is OsmQuestKey && key.elementType == ElementType.WAY && key.elementId == SIDEWALK.id
            }
            quest != null
        }
        return quest!!
    }

    /** Taps the quest's pin on the map, where a finger would (the pin is drawn just above its
     *  anchor), rather than calling the map's click listener. */
    private fun tapQuestPin(activity: MainActivity, quest: Quest) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // pins are only drawn at street level - zoom in on the quest, as the user would
        instrumentation.runOnMainSync {
            val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.mapFragment) as MainMapFragment
            mapFragment.isFollowingPosition = false
            mapFragment.updateCameraPosition {
                position = quest.position
                zoom = 19.0
            }
        }
        SystemClock.sleep(2_000) // let the pins render at the new zoom
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}.zoomed")
        var x = 0f
        var y = 0f
        waitUntil("map projection ready") {
            var ok = false
            instrumentation.runOnMainSync {
                val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.mapFragment) as MainMapFragment?
                val point = mapFragment?.getPointOf(quest.position)
                val view = mapFragment?.view
                if (point != null && view != null) {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val density = activity.resources.displayMetrics.density
                    x = location[0] + point.x
                    y = location[1] + point.y - PIN_OFFSET_DP * density
                    ok = point.x in 0f..view.width.toFloat() && point.y in 0f..view.height.toFloat()
                }
            }
            ok
        }
        val downTime = SystemClock.uptimeMillis()
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0))
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0))
    }

    private fun answerConcreteAndWidth60() {
        tapTile(SURFACE_Q, "Concrete")
        typeInto(WIDTH_Q, "60")
        submit()
    }

    private fun awaitConflictDialog() = waitForText(CONFLICT_DIALOG_TITLE)

    private fun assertNoConflictDialog() {
        assertTrue(composeTestRule.onAllNodes(hasText(CONFLICT_DIALOG_TITLE)).fetchSemanticsNodes().isEmpty())
    }

    private fun tapTile(question: String, choice: String) {
        onView(tileOf(question, choice)).perform(scrollIntoView(), click())
    }

    private fun typeInto(question: String, text: String) {
        expandSheet()
        onView(fieldOf(question)).perform(scrollIntoView(), replaceText(text), closeSoftKeyboard())
    }

    /** The quest sheet opens half-way over the map; swipe it up like a user would. */
    private fun expandSheet() {
        // dragged by its title (the part that's always on screen) to the top of the screen
        val toTop = CoordinatesProvider { view ->
            val xy = IntArray(2).also { view.getLocationOnScreen(it) }
            floatArrayOf(xy[0] + view.width / 2f, 50f)
        }
        onView(withText(containsString("Way #${SIDEWALK.id}")))
            .perform(GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER, toTop, Press.FINGER))
    }

    private fun submit() {
        expandSheet()
        onView(withId(R.id.submitButton)).perform(scrollIntoView(), click())
    }

    private fun isDisplayed(questionTitle: String): Boolean = try {
        onView(de.westnordost.streetcomplete.quests.sidewalk_long_form.questionRow(questionTitle)).check(matches(isDisplayed()))
        true
    } catch (e: Throwable) {
        false
    }

    private fun awaitUploads(count: Int): List<MockOsmServer.Upload> {
        waitUntil("$count upload(s)") { MockOsmServer.snapshot().uploads.size >= count }
        return MockOsmServer.snapshot().uploads
    }

    private fun awaitChangesetsClosed() {
        waitUntil("all changesets closed") { MockOsmServer.snapshot().changesets.none { it.isOpen } }
    }

    private fun assertNoUnexpectedRequests() {
        assertEquals(emptyList<String>(), MockOsmServer.snapshot().unexpectedRequests)
    }

    //endregion

    //region waiting

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeTestRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Polls [condition] while letting both Compose and the main looper run. */
    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) {
                UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}.timeout")
                throw AssertionError("Timed out waiting for: $what\nOSM requests: ${MockOsmServer.snapshot().requests}")
            }
            try { composeTestRule.waitForIdle() } catch (_: IllegalStateException) {}
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(200)
        }
    }

    private fun <T : Activity> awaitActivity(type: Class<T>): T {
        var activity: T? = null
        waitUntil("${type.simpleName} resumed") {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance(type)
                    .firstOrNull()
            }
            activity != null
        }
        return activity!!
    }

    //endregion

    //region setup

    private fun workspaceApiService(): WorkspaceApiService {
        val engine = MockEngine { request ->
            val body = if (request.url.encodedPath.endsWith("/mine")) WORKSPACE_LIST_JSON else workspaceDetailsJson()
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return WorkspaceApiService(client, preferences, koin.get(), koin.get<WorkspaceConfigProvider>(), client)
    }

    private fun workspaceDetailsJson() = """{
        "id": $WORKSPACE_ID, "title": "${FakeWorkspaceRepository.TEST_WORKSPACE_TITLE}", "type": "osw",
        "description": null, "createdAt": "2025-01-01T00:00:00Z", "createdBy": "user-1",
        "createdByName": "Jane Doe", "externalAppAccess": 1, "overrideConflicts": $overrideConflicts,
        "imageryListDef": null, "kartaViewToken": null, "longFormQuestDef": $LONG_FORM,
        "tdeiMetadata": null, "tdeiProjectGroupId": null, "tdeiRecordId": null, "tdeiServiceId": null
    }"""

    private fun saveValidSession() {
        val now = System.currentTimeMillis()
        preferences.workspaceLogin = true
        preferences.workspaceToken = "saved-access"
        preferences.workspaceRefreshToken = "saved-refresh"
        preferences.workspaceUserEmail = "test.user@example.com"
        preferences.workspaceUserId = FakeWorkspaceRepository.TEST_USER_ID
        preferences.workspaceLastLogin = now
        preferences.accessTokenExpiryInterval = DAY
        preferences.refreshTokenExpiryInterval = DAY
        preferences.accessTokenExpiryTime = now + DAY
        preferences.refreshTokenExpiryTime = now + DAY
        preferences.isBiometricEnabled = false
        preferences.autosync = Autosync.ON
    }

    /** No edits, conflicts, open changesets or downloaded areas left over from a previous test. */
    private fun clearAppState() {
        awaitBackgroundSyncStopped()
        val db: Database = koin.get()
        db.delete(ElementEditsTable.NAME)
        db.delete(PendingTagConflictsTable.NAME)
        db.delete(OpenChangesetsTable.NAME)
        koin.get<DownloadedTilesController>().invalidateAll()
        workspaceDao.deleteAll(workspaceDao.getAll().map { it.id })
        preferences.workspaceId = null
        preferences.workspaceLogin = false
        preferences.workspaceToken = null
        preferences.workspaceRefreshToken = null
    }

    /** A download or upload still running from the previous test would otherwise finish after
     *  the reset below - e.g. marking the area as downloaded again, so this test's download never
     *  happens. */
    private fun awaitBackgroundSyncStopped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        WorkManager.getInstance(context).cancelAllWork().result.get()
        val downloads: DownloadProgressSource = koin.get()
        val uploads: UploadProgressSource = koin.get()
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (downloads.isDownloadInProgress || uploads.isUploadInProgress) {
            check(SystemClock.uptimeMillis() < deadline) { "background download/upload didn't stop" }
            SystemClock.sleep(100)
        }
    }

    private fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().decodeToString() }
    }

    private fun finishAllActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
    }

    //endregion

    private companion object {
        const val TIMEOUT = 45_000L
        val ANIMATION_SCALES = listOf("animator_duration_scale", "transition_animation_scale", "window_animation_scale")
        const val DAY = 24 * 60 * 60 * 1000L
        const val PIN_OFFSET_DP = 20
        const val WORKSPACE_ID = 1

        const val CONFLICT_DIALOG_TITLE = "Resolve Conflicts"
        const val SURFACE_Q = "What is the surface?"
        const val WIDTH_Q = "How wide is it, in inches?"

        val USER_POSITION = LatLon(47.65530, -122.30350)
        val AREA = BoundingBox(47.654, -122.305, 47.657, -122.302)

        val SIDEWALK_NODES = listOf(
            MockNode(1, 47.65530, -122.30380),
            MockNode(2, 47.65530, -122.30320),
        )
        val SIDEWALK = MockWay(
            id = 100, nodeIds = listOf(1, 2),
            tags = mapOf("highway" to "footway", "footway" to "sidewalk", "surface" to "asphalt"),
        )

        val WORKSPACE_LIST_JSON = """[{"id":$WORKSPACE_ID,"title":"${FakeWorkspaceRepository.TEST_WORKSPACE_TITLE}","type":"osw","externalAppAccess":1,"createdAt":"2025-01-01T00:00:00Z"}]"""

        val LONG_FORM = """{
          "version": "3.2.0",
          "recency_period": 90,
          "elements": [{
            "element_type": "Sidewalks",
            "element_type_icon": "sidewalk",
            "quest_query": "ways with highway=footway and footway=sidewalk",
            "quests": [
              {"quest_id": 101, "quest_title": "$SURFACE_Q", "quest_type": "ExclusiveChoice", "quest_tag": "surface",
               "quest_answer_choices": [{"value": "asphalt", "choice_text": "Asphalt"},
                                        {"value": "concrete", "choice_text": "Concrete"},
                                        {"value": "gravel", "choice_text": "Gravel"}]},
              {"quest_id": 102, "quest_title": "$WIDTH_Q", "quest_type": "Numeric", "quest_tag": "width",
               "quest_answer_validation": {"min": 12, "max": 240}}
            ]
          }]
        }"""
    }
}
