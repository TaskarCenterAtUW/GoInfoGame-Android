package de.westnordost.streetcomplete.screens.workspaces

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Parcel
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.content.IntentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.preferences.EnvironmentManager
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.user.WorkspaceConfigProvider
import de.westnordost.streetcomplete.data.workspace.Workspace
import de.westnordost.streetcomplete.data.workspace.WorkspaceDao
import de.westnordost.streetcomplete.data.workspace.data.remote.WorkspaceApiService
import de.westnordost.streetcomplete.data.workspace.data.repository.WorkspaceRepositoryImpl
import de.westnordost.streetcomplete.data.workspace.domain.WorkspaceRepository
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.Elements
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.LongFormResponse
import de.westnordost.streetcomplete.screens.main.MainActivity
import de.westnordost.streetcomplete.testutils.UiTestScreenshot
import de.westnordost.streetcomplete.util.satellite_layers.Imagery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.util.Collections

/**
 * After logging in: the workspace list shows, the user taps a workspace, its details (long form +
 * conflict mode) are fetched, and the map (MainActivity) is opened with that long form.
 *
 * Login itself stays on [FakeWorkspaceRepository], but the workspace list and details go through
 * the real [WorkspaceApiService] -> [WorkspaceRepositoryImpl] -> [WorkspaceDao] against a mock
 * HTTP server, so the JSON parsing and the `overrideConflicts` write to the workspace table are
 * the app's own. Starting MainActivity is stubbed (Espresso-Intents) - what matters is the
 * intent it would get.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceLongFormFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *(if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        })
    )

    // see WorkspaceLoginFlowTest for why the activity is launched manually
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testName = TestName()

    private val koin = GlobalContext.get()
    private val preferences: Preferences = koin.get()
    private val workspaceDao: WorkspaceDao = koin.get()

    private val fakeRepository = FakeWorkspaceRepository()
    private lateinit var scenario: ActivityScenario<WorkSpaceActivity>

    /** What the mock server answers GET {workspaceBaseUrl}/{id} with. */
    @Volatile private var detailsResponse: (id: Int) -> Pair<HttpStatusCode, String> =
        { HttpStatusCode.OK to workspaceDetailsJson(it) }
    private val requests: MutableList<HttpRequestData> = Collections.synchronizedList(mutableListOf())

    @Before
    fun setUp() {
        enableDeviceLocation()
        clearSession()
        preferences.isBiometricEnabled = false
        preferences.workspaceId = null
        workspaceDao.deleteAll(workspaceDao.getAll().map { it.id })

        val realRepository = WorkspaceRepositoryImpl(workspaceApiService(), workspaceDao)
        fakeRepository.workspaces = { realRepository.getWorkspaces(it) }
        fakeRepository.workspaceDetails = { realRepository.getWorkspaceDetails(it) }
        loadKoinModules(module {
            single<WorkspaceRepository> { fakeRepository }
        })

        Intents.init()
        intending(hasComponent(MainActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        UiTestScreenshot.capture("${javaClass.simpleName}.${testName.methodName}")
        Intents.release()
        if (::scenario.isInitialized) scenario.close()
        finishAllActivities()
        clearSession()
        preferences.workspaceId = null
        workspaceDao.deleteAll(workspaceDao.getAll().map { it.id })
    }

    //region valid long form

    @Test
    fun tappingWorkspace_fetchesItsDetails_withTheLoggedInToken() {
        logInAndOpenWorkspace()
        awaitMapLaunch()

        assertEquals(listOf(WORKSPACE_ID), fakeRepository.workspaceDetailsRequests)
        val request = requests.single { !it.url.encodedPath.endsWith("/mine") }
        assertEquals(
            EnvironmentManager(preferences).currentEnvironment.workspaceBaseUrl + "/$WORKSPACE_ID",
            request.url.toString()
        )
        assertEquals(
            "Bearer ${FakeWorkspaceRepository.TEST_LOGIN_RESPONSE.access_token}",
            request.headers[HttpHeaders.Authorization]
        )
    }

    @Test
    fun tappingWorkspace_opensMap_withTheValidLongForm() {
        logInAndOpenWorkspace()
        // as MainActivity will receive it - the extras are parcelled across the process boundary
        val intent = awaitMapLaunch().parcelled()

        val expected = Json { ignoreUnknownKeys = true }.decodeFromString<LongFormResponse>(VALID_LONG_FORM)
        val longForm = IntentCompat.getParcelableArrayListExtra(intent, "LONG_FORM", Elements::class.java)
        assertNotNull("LONG_FORM extra missing", longForm)
        assertValidLongForm(longForm!!)
        assertEquals(expected.elements, longForm)

        assertEquals(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE, intent.getStringExtra("WORKSPACE_TITLE"))
        assertEquals(30, intent.getIntExtra("RECENCY_PERIOD_IN_DAYS", -1))
        assertEquals(
            expected.featurePresets,
            IntentCompat.getParcelableArrayListExtra(intent, "FEATURE_PRESETS", FeaturePreset::class.java)
        )
        assertEquals(
            expected.customIcons,
            IntentCompat.getParcelableArrayListExtra(intent, "CUSTOM_ICONS", CustomIcon::class.java)
        )
        assertTrue(IntentCompat.getParcelableArrayListExtra(intent, "IMAGERY_LIST", Imagery::class.java)!!.isEmpty())

        assertEquals(WORKSPACE_ID, preferences.workspaceId)
        assertTrue(preferences.showLongForm)
    }

    //endregion

    //region overrideConflicts

    @Test
    fun overrideConflictsTrue_isStoredForTheWorkspace() {
        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, overrideConflicts = "true") }
        logInAndOpenWorkspace()
        awaitMapLaunch()
        assertEquals(true, storedWorkspace().overrideConflicts)
    }

    @Test
    fun overrideConflictsFalse_isStoredForTheWorkspace() {
        // a previous visit stored OVERRIDE - the details now say otherwise
        workspaceDao.put(listOf(testWorkspace(overrideConflicts = true)))
        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, overrideConflicts = "false") }
        logInAndOpenWorkspace()
        awaitMapLaunch()
        assertEquals(false, storedWorkspace().overrideConflicts)
    }

    @Test
    fun overrideConflictsMissing_isStoredAsResolve() {
        workspaceDao.put(listOf(testWorkspace(overrideConflicts = true)))
        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, overrideConflicts = null) }
        logInAndOpenWorkspace()
        awaitMapLaunch()
        assertEquals(false, storedWorkspace().overrideConflicts)
    }

    @Test
    fun workspaceListRefresh_keepsTheStoredMode() {
        // OVERRIDE stored by an earlier visit; the list endpoint doesn't return the field
        workspaceDao.put(listOf(testWorkspace(overrideConflicts = true)))
        launch()
        logIn()
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        assertEquals(true, storedWorkspace().overrideConflicts)
    }

    //endregion

    //region invalid long form / failures

    @Test
    fun longFormWithoutElements_showsError_andStaysOnList() {
        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, longFormQuestDef = """{"version":"3.2.0","elements":[]}""") }
        logInAndOpenWorkspace()

        waitForText("Error: No long form quests available for this workspace")
        assertMapNotLaunched()
    }

    @Test
    fun noLongForm_showsError_andStaysOnList() {
        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, longFormQuestDef = "null") }
        logInAndOpenWorkspace()

        waitForText("Error: Unexpected JSON structure for long form")
        assertMapNotLaunched()
    }

    @Test
    fun longFormWithUnparseableQuestQuery_showsError() {
        detailsResponse = {
            HttpStatusCode.OK to workspaceDetailsJson(it, longFormQuestDef = VALID_LONG_FORM.replace(QUEST_QUERY, "ways with (highway="))
        }
        logInAndOpenWorkspace()

        waitForText("Error: Workspace is not configured properly")
        assertMapNotLaunched()
    }

    // regression: this threw out of the ViewModel's flow (below its catch{}) and crashed the app
    @Test
    fun longFormOfTheWrongShape_showsError_insteadOfCrashing() {
        detailsResponse = {
            HttpStatusCode.OK to workspaceDetailsJson(it, longFormQuestDef = """{"version":"3.2.0","elements":[{"element_type":"Sidewalks","quests":"not a list"}]}""")
        }
        logInAndOpenWorkspace()

        waitForText("Error: Workspace is not configured properly")
        assertMapNotLaunched()
    }

    // regression: the raw "Illegal input: Fields [...] are required ..." was shown
    @Test
    fun incompleteWorkspaceDetails_showsMisconfiguredWorkspaceError() {
        detailsResponse = { HttpStatusCode.OK to """{"id":$it,"title":"${FakeWorkspaceRepository.TEST_WORKSPACE_TITLE}"}""" }
        logInAndOpenWorkspace()

        waitForText("Error: Workspace is not configured properly. Please contact the Admin for the workspace")
        assertMapNotLaunched()
    }

    @Test
    fun workspaceNotFound_showsError_andRetryOpensIt() {
        detailsResponse = { HttpStatusCode.NotFound to """{"message":"not found"}""" }
        logInAndOpenWorkspace()
        waitForText("Error: Failed. Workspace not found with ID : $WORKSPACE_ID")
        assertMapNotLaunched()

        detailsResponse = { HttpStatusCode.OK to workspaceDetailsJson(it, overrideConflicts = "true") }
        // regression: the failure clears the selected workspace, and once that reached the screen
        // "Retry" retried with no workspace - i.e. did nothing. Settle first so it always has.
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Retry").performClick()

        awaitMapLaunch()
        assertEquals(listOf(WORKSPACE_ID, WORKSPACE_ID), fakeRepository.workspaceDetailsRequests)
        assertEquals(true, storedWorkspace().overrideConflicts)
    }

    @Test
    fun serverErrorOnWorkspaceDetails_showsFriendlyError() {
        detailsResponse = { HttpStatusCode.InternalServerError to """{"message":"boom"}""" }
        logInAndOpenWorkspace()

        waitForText("Error: The server is temporarily unavailable. Please try again later.")
        assertMapNotLaunched()
    }

    //endregion

    //region helpers

    private fun workspaceApiService(): WorkspaceApiService {
        val engine = MockEngine { request ->
            requests.add(request)
            val (status, body) = if (request.url.encodedPath.endsWith("/mine")) {
                HttpStatusCode.OK to WORKSPACE_LIST_JSON
            } else {
                detailsResponse(request.url.encodedPath.substringAfterLast('/').toInt())
            }
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        // same plugins the real client is set up with in ApplicationModule
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Auth) { bearer { loadTokens { null } } }
        }
        return WorkspaceApiService(client, preferences, koin.get(), koin.get<WorkspaceConfigProvider>(), client)
    }

    private fun launch() {
        scenario = ActivityScenario.launch(WorkSpaceActivity::class.java)
    }

    private fun logIn() {
        composeTestRule.onNodeWithText("Email").performTextInput(TEST_EMAIL)
        composeTestRule.onNodeWithText("Password").performTextInput(TEST_PASSWORD)
        // see WorkspaceLoginFlowTest.logInAndWaitForWorkspaceList
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("Login").performClick()
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithContentDescription(PROFILE_NAV_DESCRIPTION).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun logInAndOpenWorkspace() {
        launch()
        logIn()
        // the list only loads once a location fix comes in
        waitForText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE)
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).performClick()
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun mapLaunches(): List<Intent> =
        Intents.getIntents().filter { it.component?.className == MainActivity::class.java.name }

    /** Waits for the (stubbed) start of MainActivity and returns the intent it was started with. */
    private fun awaitMapLaunch(): Intent {
        // not a Thread.sleep() loop - the composition only advances while the compose rule is
        // waiting, so the LaunchedEffect that starts MainActivity would never get to run
        composeTestRule.waitUntil(timeoutMillis = 20_000) { mapLaunches().isNotEmpty() }
        return mapLaunches().first()
    }

    private fun assertMapNotLaunched() {
        assertTrue("MainActivity must not be started", mapLaunches().isEmpty())
        composeTestRule.onNodeWithText(FakeWorkspaceRepository.TEST_WORKSPACE_TITLE).assertExists()
    }

    private fun Intent.parcelled(): Intent {
        val parcel = Parcel.obtain()
        try {
            writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Intent.CREATOR.createFromParcel(parcel).also {
                it.setExtrasClassLoader(Elements::class.java.classLoader)
            }
        } finally {
            parcel.recycle()
        }
    }

    private fun storedWorkspace(): Workspace = workspaceDao.get(WORKSPACE_ID.toLong()).single()

    private fun testWorkspace(overrideConflicts: Boolean) = Workspace(
        id = WORKSPACE_ID,
        title = FakeWorkspaceRepository.TEST_WORKSPACE_TITLE,
        type = "osw",
        externalAppAccess = 1,
        overrideConflicts = overrideConflicts,
    )

    /** Same checks as the unit tests' assertValidLongForm: what the map and form rely on. */
    private fun assertValidLongForm(elements: List<Elements>) {
        assertTrue("long form has no elements", elements.isNotEmpty())
        for (element in elements) {
            val name = element.elementType
            assertFalse("element without element_type", name.isNullOrBlank())
            element.questQuery!!.toElementFilterExpression()
            val quests = element.quests.map { assertNotNull("$name: null question", it); it!! }
            assertTrue("$name: no questions", quests.isNotEmpty())
            val ids = quests.map { it.questId }
            assertEquals("$name: duplicate quest_id in $ids", ids.size, ids.toSet().size)
            for (quest in quests) {
                val label = "$name/${quest.questId}"
                assertNotNull("$label: no quest_id", quest.questId)
                assertFalse("$label: no quest_tag", quest.questTag.isNullOrBlank())
                assertFalse("$label: no quest_title", quest.questTitle.isNullOrBlank())
                assertTrue("$label: unknown quest_type ${quest.questType}", quest.questType in KNOWN_QUEST_TYPES)
                if (quest.questType == "ExclusiveChoice" || quest.questType == "MultipleChoice") {
                    assertFalse("$label: no choices", quest.questAnswerChoices.isNullOrEmpty())
                }
                quest.questAnswerDependency?.forEach {
                    assertTrue("$label: depends on unknown question ${it.questionId}", it.questionId in ids)
                }
            }
            // unanswered and visible when the form first opens
            assertTrue("$name: arrived pre-answered", quests.all { it.userInput == null && it.selectedIndex == null && it.visible })
        }
    }

    private fun clearSession() {
        preferences.workspaceLogin = false
        preferences.workspaceToken = null
        preferences.workspaceRefreshToken = null
        preferences.workspaceUserId = null
        preferences.workspaceUserEmail = null
        preferences.accessTokenExpiryTime = 0L
        preferences.refreshTokenExpiryTime = 0L
    }

    private fun finishAllActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
    }

    private fun enableDeviceLocation() {
        try {
            val pfd: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("cmd location set-location-enabled true")
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        } catch (e: Exception) {
            // best-effort, see WorkspaceLoginFlowTest.enableDeviceLocation
        }
    }

    //endregion

    private companion object {
        const val WORKSPACE_ID = 1
        const val TEST_EMAIL = "test.user@example.com"
        const val TEST_PASSWORD = "Test@1234"
        const val PROFILE_NAV_DESCRIPTION = "Navigate to profile screen"
        const val QUEST_QUERY = "ways with (highway=footway and footway=sidewalk)"
        val KNOWN_QUEST_TYPES = setOf("ExclusiveChoice", "MultipleChoice", "TextEntry", "Numeric")

        val WORKSPACE_LIST_JSON = """[{"id":$WORKSPACE_ID,"title":"${FakeWorkspaceRepository.TEST_WORKSPACE_TITLE}","type":"osw","externalAppAccess":1,"createdAt":"2025-01-01T00:00:00Z"}]"""

        val VALID_LONG_FORM = """{
          "version": "3.2.0",
          "recency_period": 30,
          "feature-presets": [{"name": "Bench", "icon": "preset_temaki_bench", "tags": {"amenity": "bench"}}],
          "custom-icons": [{"name": "streetlight", "url": "https://pinhead.ink/v25/lantern_lamppost.svg", "type": "feature-preset"}],
          "elements": [{
            "element_type": "Sidewalks",
            "element_type_icon": "sidewalk",
            "quest_query": "$QUEST_QUERY",
            "quests": [
              {"quest_id": 101, "quest_title": "Surface?", "quest_type": "ExclusiveChoice", "quest_tag": "ext:surface",
               "quest_answer_choices": [{"value": "asphalt", "choice_text": "Asphalt"}, {"value": "other", "choice_text": "Other"}]},
              {"quest_id": 102, "quest_title": "Describe the surface", "quest_type": "TextEntry", "quest_tag": "ext:surface:description",
               "quest_answer_dependency": {"question_id": 101, "required_value": "other"}},
              {"quest_id": 103, "quest_title": "Width?", "quest_type": "Numeric", "quest_tag": "width",
               "quest_answer_validation": {"min": 12, "max": 240}},
              {"quest_id": 104, "quest_title": "Obstructions?", "quest_type": "MultipleChoice", "quest_tag": "ext:obstruction:type",
               "quest_answer_choices": [{"value": "bollard", "choice_text": "Bollard"},
                                        {"value": "other", "choice_text": "Other", "choice_follow_up": "Please take a photo of the obstruction."}]}
            ]
          }]
        }"""

        /** [overrideConflicts] is raw JSON; null leaves the key out, as older backends do. */
        fun workspaceDetailsJson(
            id: Int,
            overrideConflicts: String? = null,
            longFormQuestDef: String = VALID_LONG_FORM,
        ): String {
            val overrideField = overrideConflicts?.let { """"overrideConflicts": $it,""" } ?: ""
            return """{
              "id": $id, "title": "${FakeWorkspaceRepository.TEST_WORKSPACE_TITLE}", "type": "osw",
              "description": null, "createdAt": "2025-01-01T00:00:00Z", "createdBy": "user-1",
              "createdByName": "Jane Doe", "externalAppAccess": 1, $overrideField
              "imageryListDef": null, "kartaViewToken": null, "longFormQuestDef": $longFormQuestDef,
              "tdeiMetadata": null, "tdeiProjectGroupId": null, "tdeiRecordId": null, "tdeiServiceId": null
            }"""
        }
    }
}
