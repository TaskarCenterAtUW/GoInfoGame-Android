package de.westnordost.streetcomplete.screens.main

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.IntentCompat
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
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.download.DownloadProgressSource
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.osm.edits.DiscardedEditNoticesTable
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsTable
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosTable
import de.westnordost.streetcomplete.data.osm.edits.create_feature.StuckPhotoUploadNoticesTable
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
import de.westnordost.streetcomplete.quests.sidewalk_long_form.inRowOf
import de.westnordost.streetcomplete.quests.sidewalk_long_form.scrollIntoView
import de.westnordost.streetcomplete.quests.sidewalk_long_form.tileOf
import de.westnordost.streetcomplete.screens.main.map.MainMapFragment
import de.westnordost.streetcomplete.screens.workspaces.FakeWorkspaceRepository
import de.westnordost.streetcomplete.screens.workspaces.WorkSpaceActivity
import de.westnordost.streetcomplete.testutils.MockGps
import de.westnordost.streetcomplete.testutils.MockKartaView
import de.westnordost.streetcomplete.testutils.MockOsmServer
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockNode
import de.westnordost.streetcomplete.testutils.MockOsmServer.MockWay
import de.westnordost.streetcomplete.testutils.ToastWatcher
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
 * Shared setup for the end-to-end tests of a long-form answer's way to OSM on the real map
 * screen: logged in (saved session), the workspace list shows, the user taps the workspace,
 * MainActivity opens with its long form, [MockGps] puts the user next to a sidewalk, the map data
 * is downloaded from [MockOsmServer], and the sidewalk's quest pin can be tapped on the map.
 *
 * Everything past the fake login is the app's own code. The network is [MockOsmServer] and
 * [MockKartaView], both hooked into the app's own HttpClients (see [MockOsmServer.attachTo] for
 * why), and a mock workspace API behind the real [WorkspaceApiService] / [WorkspaceRepositoryImpl]
 * / [WorkspaceDao].
 */
abstract class MapSyncTestBase {

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

    protected val koin = GlobalContext.get()
    protected val preferences: Preferences = koin.get()
    protected val workspaceDao: WorkspaceDao = koin.get()
    private lateinit var gps: MockGps
    protected lateinit var toasts: ToastWatcher
    protected val fakeRepository = FakeWorkspaceRepository()
    protected lateinit var scenario: ActivityScenario<WorkSpaceActivity>

    @Volatile protected var overrideConflicts: String = "null"
    private var previousAnimationScales: List<String> = emptyList()

    @Before
    fun setUp() {
        clearAppState()
        saveValidSession()

        MockOsmServer.reset()
        MockOsmServer.put(*sidewalkNodes.toTypedArray(), sidewalk, *otherElements.toTypedArray())
        MockKartaView.reset()

        val realRepository = WorkspaceRepositoryImpl(workspaceApiService(), workspaceDao)
        fakeRepository.workspaces = { realRepository.getWorkspaces(it) }
        fakeRepository.workspaceDetails = { realRepository.getWorkspaceDetails(it) }
        loadKoinModules(module {
            single<WorkspaceRepository> { fakeRepository }
        })
        MockOsmServer.attachTo(koin.get(named("osmClient")))
        MockOsmServer.isActive = true
        // photo uploads must never reach the real KartaView either
        MockKartaView.attachTo(koin.get(named("kartaViewClient")))
        MockKartaView.isActive = true

        // photo thumbnails of existing answers are real https URLs (loaded by Coil, which the
        // network guard doesn't cover) - never fetch them in tests
        wasLowBandwidth = preferences.isLowBandwidthModeEnabled
        preferences.isLowBandwidthModeEnabled = true

        gps = MockGps(userPosition.latitude, userPosition.longitude)
        gps.start()
        toasts = ToastWatcher()
        // same as LongFormFormTest: RecyclerView item animations leave two views of the same
        // tile around for a moment, which Espresso doesn't wait for
        previousAnimationScales = ANIMATION_SCALES.map { shell("settings get global $it").trim() }
        ANIMATION_SCALES.forEach { shell("settings put global $it 0") }
    }

    @After
    fun tearDown() {
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        toasts.close()
        gps.stop()
        ANIMATION_SCALES.zip(previousAnimationScales).forEach { (key, value) ->
            shell("settings put global $key ${value.takeIf { it != "null" } ?: "1"}")
        }
        if (::scenario.isInitialized) scenario.close()
        finishAllActivities()
        clearAppState()
        MockOsmServer.isActive = false
        MockKartaView.isActive = false
        preferences.isLowBandwidthModeEnabled = wasLowBandwidth
    }

    /** Where the (mock) GPS puts the user, and the sidewalk the tests answer - overridable for a
     *  test that needs its own area. */
    protected open val userPosition: LatLon = USER_POSITION
    protected open val sidewalkNodes: List<MockNode> = SIDEWALK_NODES
    protected open val sidewalk: MockWay = SIDEWALK
    protected open val area: BoundingBox = AREA
    /** More map data around the sidewalk (e.g. another sidewalk). */
    protected open val otherElements: List<Any> = emptyList()
    private var wasLowBandwidth = false

    //region flow helpers

    protected fun openWorkspaceAndSidewalkQuest() {
        openSidewalkQuest(openWorkspace())
    }

    /** From the workspace list (saved session) into the map screen of the workspace. */
    protected fun openWorkspace(): MainActivity {
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).performClick()
        return awaitActivity(MainActivity::class.java)
    }

    protected fun openSidewalkQuest(activity: MainActivity) {
        waitUntil("map data downloaded") { MockOsmServer.snapshot().requests.any { it == "GET map" } }
        val quest = awaitSidewalkQuest()
        tapQuestPin(activity, quest)
        waitUntil("long form opened") { isDisplayed(SURFACE_Q) }
    }

    /** The toolbar's upload button - a manual upload (and re-opens a postponed conflict sheet). */
    protected fun tapUploadButton() {
        onView(withId(R.id.uploadButton)).perform(click())
    }

    protected fun unsyncedEditsCount(): Int = koin.get<ElementEditsController>().getUnsyncedCount()

    protected fun awaitToast(resId: Int) {
        val text = InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)
        waitUntil("toast \"$text\"") { toasts.texts.any { it.contains(text) } } // Android 12+ appends the app name
    }

    protected fun awaitText(text: String) = waitForText(text)

    protected fun isShown(text: String): Boolean =
        composeTestRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    protected fun awaitSidewalkQuest(): Quest = awaitQuestFor(sidewalk.id)

    protected fun awaitQuestFor(wayId: Long): Quest {
        val source: VisibleQuestsSource = koin.get()
        var quest: Quest? = null
        waitUntil("quest for way $wayId on the map") {
            quest = questFor(source, wayId)
            quest != null
        }
        return quest!!
    }

    protected fun questFor(source: VisibleQuestsSource, wayId: Long): Quest? =
        source.getAll(area).firstOrNull {
            val key = it.key
            key is OsmQuestKey && key.elementType == ElementType.WAY && key.elementId == wayId
        }

    /** Taps the quest's pin on the map, where a finger would (the pin is drawn just above its
     *  anchor), rather than calling the map's click listener. */
    protected fun tapQuestPin(activity: MainActivity, quest: Quest) {
        zoomTo(activity, quest.position)
        val (x, y) = pinOnScreen(activity, quest)
        press(x, y, long = false)
    }

    /** Pins are only drawn at street level - zoom in on [position], as the user would. */
    protected fun zoomTo(activity: MainActivity, position: LatLon) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.mapFragment) as MainMapFragment
            mapFragment.isFollowingPosition = false
            mapFragment.updateCameraPosition {
                this.position = position
                zoom = 19.0
            }
        }
        SystemClock.sleep(2_000) // let the pins render at the new zoom
    }

    protected fun pinOnScreen(activity: MainActivity, quest: Quest): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        waitUntil("pin of ${quest.key} on screen") {
            var ok = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
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
        return x to y
    }

    /** A finger on the screen at [x],[y] - a tap, or held long enough to count as a long press. */
    protected fun press(x: Float, y: Float, long: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0))
        if (long) SystemClock.sleep(ViewConfiguration.getLongPressTimeout() * 2L)
        instrumentation.sendPointerSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0))
    }

    /** Needs Intents.init(). The camera is stubbed to "take" a small real JPEG (the form reads its EXIF). */
    protected fun capturePhoto() {
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            val uri = IntentCompat.getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)!!
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.contentResolver.openOutputStream(uri)!!.use {
                Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
        onView(inRowOf(OBSTRUCTION_Q, R.id.choice_follow_up)).perform(scrollIntoView(), click())
        // the camera result comes back asynchronously - submitting before the photo card shows
        // would submit the answer without its photo
        waitUntil("photo attached") {
            try {
                onView(inRowOf(OBSTRUCTION_Q, R.id.photo_title)).perform(scrollIntoView()).check(matches(withText("Photo attached")))
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    protected fun answerConcreteAndWidth60() {
        tapTile(SURFACE_Q, "Concrete")
        typeInto(WIDTH_Q, "60")
        submit()
    }

    protected fun awaitConflictDialog() = waitForText(CONFLICT_DIALOG_TITLE)

    protected fun assertNoConflictDialog() {
        assertTrue(composeTestRule.onAllNodes(hasText(CONFLICT_DIALOG_TITLE)).fetchSemanticsNodes().isEmpty())
    }

    protected fun tapTile(question: String, choice: String) {
        expandSheet()
        onView(tileOf(question, choice)).perform(scrollIntoView(), click())
    }

    protected fun typeInto(question: String, text: String) {
        expandSheet()
        onView(fieldOf(question)).perform(scrollIntoView(), replaceText(text), closeSoftKeyboard())
    }

    /** The quest sheet opens half-way over the map; swipe it up like a user would. */
    protected fun expandSheet() {
        // dragged by its title (the part that's always on screen) to the top of the screen
        val toTop = CoordinatesProvider { view ->
            val xy = IntArray(2).also { view.getLocationOnScreen(it) }
            floatArrayOf(xy[0] + view.width / 2f, 50f)
        }
        onView(withText(containsString("Way #${sidewalk.id}")))
            .perform(GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER, toTop, Press.FINGER))
        // the sheet keeps settling after the finger lifts (ViewDragHelper - not an animation
        // Espresso waits for), and a tap on a settling sheet only stops it: the next click (e.g.
        // Submit) would be swallowed. Whether it settles at all depends on the sheet's height, i.e.
        // on the navigation bar - it did with 3-button navigation, not with gesture navigation.
        waitUntil("quest sheet expanded") {
            var state = BottomSheetBehavior.STATE_SETTLING
            onView(withId(R.id.bottomSheet)).check { view, _ -> state = BottomSheetBehavior.from(view).state }
            state == BottomSheetBehavior.STATE_EXPANDED
        }
    }

    protected fun submit() {
        expandSheet()
        onView(withId(R.id.submitButton)).perform(scrollIntoView(), click())
    }

    protected fun isDisplayed(questionTitle: String): Boolean = try {
        onView(de.westnordost.streetcomplete.quests.sidewalk_long_form.questionRow(questionTitle)).check(matches(isDisplayed()))
        true
    } catch (e: Throwable) {
        false
    }

    protected fun awaitUploads(count: Int): List<MockOsmServer.Upload> {
        waitUntil("$count upload(s)") { MockOsmServer.snapshot().uploads.size >= count }
        return MockOsmServer.snapshot().uploads
    }

    protected fun awaitChangesetsClosed() {
        waitUntil("all changesets closed") { MockOsmServer.snapshot().changesets.none { it.isOpen } }
    }

    protected fun assertNoUnexpectedRequests() {
        assertEquals(emptyList<String>(), MockOsmServer.snapshot().unexpectedRequests)
    }

    //endregion

    //region waiting

    protected fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeTestRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Polls [condition] while letting both Compose and the main looper run. */
    protected fun waitUntil(what: String, condition: () -> Boolean) {
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

    protected fun <T : Activity> awaitActivity(type: Class<T>): T {
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
    protected fun clearAppState() {
        awaitBackgroundSyncStopped()
        val db: Database = koin.get()
        db.delete(ElementEditsTable.NAME)
        db.delete(PendingTagConflictsTable.NAME)
        db.delete(OpenChangesetsTable.NAME)
        db.delete(DiscardedEditNoticesTable.NAME)
        db.delete(StuckPhotoUploadNoticesTable.NAME)
        db.delete(FeaturePhotosTable.NAME)
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
    protected fun awaitBackgroundSyncStopped() {
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

    protected fun finishAllActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
    }

    //endregion

    companion object {
        const val TIMEOUT = 45_000L
        val ANIMATION_SCALES = listOf("animator_duration_scale", "transition_animation_scale", "window_animation_scale")
        const val DAY = 24 * 60 * 60 * 1000L
        const val PIN_OFFSET_DP = 20
        const val WORKSPACE_ID = 1

        const val CONFLICT_DIALOG_TITLE = "Resolve Conflicts"
        const val SURFACE_Q = "What is the surface?"
        const val WIDTH_Q = "How wide is it, in inches?"
        const val OBSTRUCTION_Q = "Any obstructions?"
        const val NOTES_Q = "Anything else to note?"
        const val PHOTO_FOLLOW_UP = "Please take a photo of the obstruction."

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
          "feature-presets": [{"name": "Bench", "icon": "preset_temaki_bench", "tags": {"amenity": "bench"}}],
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
               "quest_answer_validation": {"min": 12, "max": 240}},
              {"quest_id": 103, "quest_title": "$OBSTRUCTION_Q", "quest_type": "MultipleChoice", "quest_tag": "obstacle",
               "quest_answer_choices": [{"value": "bollard", "choice_text": "Bollard"},
                                        {"value": "other", "choice_text": "Other obstruction", "choice_follow_up": "$PHOTO_FOLLOW_UP"}]},
              {"quest_id": 104, "quest_title": "$NOTES_Q", "quest_type": "TextEntry", "quest_tag": "note"}
            ]
          }]
        }"""
    }
}
